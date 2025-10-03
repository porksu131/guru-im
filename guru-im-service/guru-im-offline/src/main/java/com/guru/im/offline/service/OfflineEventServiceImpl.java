package com.guru.im.offline.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.*;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.mapper.OfflineEventMapper;
import com.guru.im.offline.model.pojo.OfflineEventContent;
import com.guru.im.offline.model.pojo.OfflineEventDelivery;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.model.OfflineSyncStatus;
import com.guru.im.protocol.util.MessageBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OfflineEventServiceImpl {

    private static final String EVENT_CACHE_KEY = "offline:events:";
    private static final int DEFAULT_SYNC_SIZE = 100;
    private static final int MAX_SYNC_SIZE = 500;
    private static final Logger log = LoggerFactory.getLogger(OfflineEventServiceImpl.class);

    @Autowired
    private OfflineEventMapper offlineEventMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private MQMessageSender mqMessageSender;

    @Transactional
    public void processSaveOfflineEvent(MQMessageWrapper messageWrapper) {
        try {
            log.info("Received offline event save message: {}", messageWrapper.getMessageId());

            Long userId = messageWrapper.getReceiverIds().get(0);
            List<String> offlineDevices = messageWrapper.getOfflineDeviceIds();

            // 保存离线事件到数据库
            OfflineEventContent offlineEventContent = convertOfflineEventContent(userId, messageWrapper);
            saveOfflineEvents(userId, Collections.singletonList(offlineEventContent));

            // 保存设备投递信息，状态为未投递
            for (String deviceId : offlineDevices) {
                checkAndCreateDelivery(userId, deviceId);
            }

            // 处理成功
            log.info("Successfully processed offline event save message: {}", messageWrapper.getMessageId());

        } catch (Exception e) {
            log.error("Failed to process offline event save message: {}", messageWrapper.getMessageId(), e);
        }
    }

    /**
     * 保存离线事件
     */
    @Transactional
    public void saveOfflineEvents(Long userId, List<OfflineEventContent> eventContents) {
        if (CollectionUtils.isEmpty(eventContents)) {
            return;
        }

        // 批量插入数据库
        offlineEventMapper.batchInsertEvents(eventContents);

        // 异步写入Redis缓存
        eventContents.forEach(event -> {
            String cacheKey = EVENT_CACHE_KEY + userId + ":" + event.getGlobalSeq();
            redisTemplate.opsForValue().set(cacheKey, event, 24, TimeUnit.HOURS);
        });

        log.info("Saved {} offline events for user: {}", eventContents.size(), userId);
    }

    @Transactional
    public void processCommonAck(Long id, Response response) {
        try {
            if (response.getCode() == ResponseCode.SUCCESS) {
                updateDeliveryStatusById(id, null, OfflineSyncStatus.OFFLINE_SYNC_STATUS_DELIVERED_VALUE);
            } else {
                updateDeliveryStatusById(id, null, OfflineSyncStatus.OFFLINE_SYNC_STATUS_DELIVER_FAILED_VALUE);
            }
        } catch (Exception e) {
            log.error("Process sync ack failed", e);
            throw new RuntimeException("Process sync ack failed", e);
        }
    }

    private OfflineEventContent convertOfflineEventContent(Long userId, MQMessageWrapper messageWrapper) {
        OfflineEventContent content = new OfflineEventContent();
        content.setId(snowflakeIdGenerator.nextId());
        content.setUserId(userId);
        content.setGlobalSeq(messageWrapper.getGlobalSeq());
        content.setEventType(messageWrapper.getCorrelationType().getCode());
        content.setEventContent(messageWrapper.getBody());
        content.setCreateTime(System.currentTimeMillis());
        content.setUpdateTime(System.currentTimeMillis());
        return content;
    }


    public void processOfflineEventsSyncRequest(MQMessageWrapper messageWrapper, SyncEventRequest syncRequest) {
        try {
            log.info("Received sync request: {}, user: {}, device: {}",
                    messageWrapper.getMessageId(), syncRequest.getUserId(), syncRequest.getDeviceId());

            // 处理同步请求
            SyncEventResponse syncResponse = syncEvents(syncRequest);

            // 构建响应消息
            MQMessageWrapper mqResponseWrapper = buildSyncResponseWrapper(syncResponse);

            // 发送响应到MQ
            mqMessageSender.sendAsync(mqResponseWrapper, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            updateDeliveryStatusById(
                                    syncResponse.getSyncEventId(),
                                    syncResponse.getLatestSequence(),
                                    OfflineSyncStatus.OFFLINE_SYNC_STATUS_PUSHED_VALUE
                            );
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            updateDeliveryStatusById(
                                    syncResponse.getSyncEventId(),
                                    syncResponse.getLatestSequence(),
                                    OfflineSyncStatus.OFFLINE_SYNC_STATUS_PUSH_FAILED_VALUE
                            );
                        }
                    }
            );

            log.info("Sent sync response for request, size: {}", syncResponse.getEventsCount());

        } catch (Exception e) {
            log.error("Failed to process sync request: {}", messageWrapper.getMessageId(), e);
            // 发送错误响应
            sendErrorResponse(messageWrapper, syncRequest, e.getMessage());
        }
    }

    private void checkAndCreateDelivery(Long userId, String deviceId) {
        OfflineEventDelivery offlineEventDelivery = offlineEventMapper.selectDeliveryRecord(userId, deviceId);
        if (offlineEventDelivery != null) {
            return ;
        }

        createAndSaveDelivery(userId, deviceId);
    }

    private OfflineEventDelivery getOrCreateDelivery(Long userId, String deviceId) {
        OfflineEventDelivery offlineEventDelivery = offlineEventMapper.selectDeliveryRecord(userId, deviceId);
        if (offlineEventDelivery != null) {
            return offlineEventDelivery;
        }

        return createAndSaveDelivery(userId, deviceId);
    }

    private OfflineEventDelivery createAndSaveDelivery(Long userId, String deviceId) {
        OfflineEventDelivery newOfflineEventDelivery = new OfflineEventDelivery();
        newOfflineEventDelivery.setId(idGenerator.nextId());
        newOfflineEventDelivery.setUserId(userId);
        newOfflineEventDelivery.setDeviceId(deviceId);
        newOfflineEventDelivery.setDeliveryStatus(OfflineSyncStatus.OFFLINE_SYNC_STATUS_IN_PROGRESS_VALUE);
        newOfflineEventDelivery.setDeliveryCount(0);
        newOfflineEventDelivery.setCreateTime(System.currentTimeMillis());
        newOfflineEventDelivery.setUpdateTime(System.currentTimeMillis());
        offlineEventMapper.insertDeliveryRecord(newOfflineEventDelivery);
        return newOfflineEventDelivery;
    }

    private MQMessageWrapper buildSyncResponseWrapper(SyncEventResponse response) {
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setSourceType(SourceType.OFFLINE_SERVICE);
        wrapper.setMessageType(MQMessageType.BATCH_SYNC);
        wrapper.setTargetTopic(MQTopic.DISPATCH_CHAT_TOPIC);
        wrapper.setReplyTopic(MQTopic.OFFLINE_TOPIC);
        wrapper.setReplyTag(MQTag.ACK);
        wrapper.setDeviceId(response.getDeviceId());
        wrapper.setCorrelationId(String.valueOf(response.getSyncEventId()));
        wrapper.setCorrelationType(CorrelationType.BATCH_EVENTS_SYNC);
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setReceiverIds(Collections.singletonList(response.getUserId()));
        wrapper.setBody(MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.OFFLINE_EVENT_RESPONSE)
                .setSyncEventResponse(response)
                .build().toByteArray());
        return wrapper;
    }

    private SyncEventResponse buildErrorResponse(SyncEventRequest request, String errorMsg) {
        SyncEventResponse.Builder builder = SyncEventResponse.newBuilder();
        builder.setUserId(request.getUserId());
        builder.setDeviceId(request.getDeviceId());
        builder.setSyncStatus(SyncStatus.SYNC_STATUS_FAILED);
        builder.setErrorMsg(errorMsg);

        return builder.build();
    }

    private void sendErrorResponse(MQMessageWrapper messageWrapper, SyncEventRequest request, String errorMsg) {
        try {
            SyncEventResponse syncEventResponse = buildErrorResponse(request, errorMsg);
            MQMessageWrapper wrapper = buildSyncResponseWrapper(syncEventResponse);
            mqMessageSender.sendOneway(wrapper);
        } catch (Exception ex) {
            log.error("Failed to send error response for request: {}", messageWrapper.getMessageId(), ex);
        }
    }

    /**
     * 同步离线事件
     */
    public SyncEventResponse syncEvents(SyncEventRequest request) {
        Long userId = request.getUserId();
        String deviceId = request.getDeviceId();
        Long lastSequence = request.getLastSequence();
        Integer syncSize = getValidSyncSize(request.getSyncSize());

        SyncEventResponse.Builder response = SyncEventResponse.newBuilder();
        response.setUserId(userId);
        response.setDeviceId(deviceId);

        try {
            // 查询当前设备的投递记录
            OfflineEventDelivery delivery = getOrCreateDelivery(userId, deviceId);
            if (lastSequence.equals(delivery.getLastSyncSeq())
                    && delivery.getDeliveryStatus() == OfflineSyncStatus.OFFLINE_SYNC_STATUS_DELIVERED_VALUE) {
                response.setSyncEventId(delivery.getId());
                response.setLatestSequence(delivery.getLastSyncSeq());
                response.addAllEvents(new ArrayList<>());
                response.setHasMore(false);
                response.setSyncStatus(SyncStatus.SYNC_STATUS_COMPLETE);
                return response.build();
            }

            // 查询事件数据（优先从Redis获取）
            List<OfflineEventContent> eventContents = getEventsFromCacheOrDB(userId, lastSequence, syncSize);

            // 转换为Protobuf事件
            List<ByteString> events = eventContents.stream()
                    .map(obj -> {
                        return ByteString.copyFrom(obj.getEventContent());
                    })
                    .collect(Collectors.toList());

            // 获取最新序列号
            long latestSequence = eventContents.stream()
                    .mapToLong(OfflineEventContent::getGlobalSeq)
                    .max()
                    .orElse(0L); // 如果集合为空，返回默认值0

            // 构建响应
            response.setSyncEventId(delivery.getId());
            response.setLatestSequence(latestSequence);
            response.addAllEvents(events);
            response.setHasMore(events.size() >= syncSize);
            response.setSyncStatus(SyncStatus.SYNC_STATUS_COMPLETE);

        } catch (Exception e) {
            log.error("Sync events failed for user: {}, device: {}", userId, deviceId, e);
            response.setSyncStatus(SyncStatus.SYNC_STATUS_FAILED);
            response.setErrorMsg("Sync failed: " + e.getMessage());
        }

        return response.build();
    }

    /**
     * 从缓存或数据库获取事件
     */
    private List<OfflineEventContent> getEventsFromCacheOrDB(Long userId, Long lastSequence, Integer limit) {
        // 先尝试从Redis获取
        List<OfflineEventContent> events = new ArrayList<>();
        long currentSeq = lastSequence + 1;
        int count = 0;

        while (count < limit) {
            String cacheKey = EVENT_CACHE_KEY + userId + ":" + currentSeq;
            OfflineEventContent event = (OfflineEventContent) redisTemplate.opsForValue().get(cacheKey);

            if (event == null) {
                break; // 缓存中没有，需要从数据库查询
            }

            events.add(event);
            currentSeq++;
            count++;
        }

        // 如果缓存中的数据不足，从数据库查询剩余部分
        if (events.size() < limit) {
            List<OfflineEventContent> dbEvents = offlineEventMapper.selectEventsByRange(
                    userId,
                    lastSequence + events.size(),
                    limit - events.size()
            );
            events.addAll(dbEvents);

            // 将数据库查询的结果缓存到Redis
            dbEvents.forEach(event -> {
                String cacheKey = EVENT_CACHE_KEY + userId + ":" + event.getGlobalSeq();
                redisTemplate.opsForValue().set(cacheKey, event, 24, TimeUnit.HOURS);
            });
        }

        return events;
    }

    /**
     * 更新投递状态
     */
    private void updateDeliveryStatus(Long userId, String deviceId, Long lastSyncSeq, Integer deliveryStatus) {
        OfflineEventDelivery delivery = new OfflineEventDelivery();
        delivery.setUserId(userId);
        delivery.setDeviceId(deviceId);
        delivery.setLastSyncSeq(lastSyncSeq);
        delivery.setDeliveryStatus(deliveryStatus);
        delivery.setLastDeliveryTime(System.currentTimeMillis());
        delivery.setDeliveryCount(1);

        offlineEventMapper.updateEventsDelivery(delivery);
    }

    private void updateDeliveryStatusById(Long id, Long lastSyncSeq, Integer deliveryStatus) {
        OfflineEventDelivery delivery = new OfflineEventDelivery();
        delivery.setId(id);
        delivery.setLastSyncSeq(lastSyncSeq);
        delivery.setLastSyncTime(System.currentTimeMillis());
        delivery.setDeliveryStatus(deliveryStatus);
        delivery.setLastDeliveryTime(System.currentTimeMillis());
        delivery.setUpdateTime(System.currentTimeMillis());

        offlineEventMapper.updateEventsDelivery(delivery);
    }


    /**
     * 获取有效的同步大小
     */
    private Integer getValidSyncSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize <= 0) {
            return DEFAULT_SYNC_SIZE;
        }
        return Math.min(requestedSize, MAX_SYNC_SIZE);
    }

    /**
     * 获取最新序列号
     */
    private Long getLatestSequence(Long userId) {
        // 先尝试从Redis获取
        String seqKey = "offline:seq:" + userId;
        Long maxSeq = (Long) redisTemplate.opsForValue().get(seqKey);

        if (maxSeq == null) {
            // 从数据库获取
            maxSeq = offlineEventMapper.selectMaxSequence(userId);
            if (maxSeq == null) {
                maxSeq = 0L;
            }
            redisTemplate.opsForValue().set(seqKey, maxSeq);
        }

        return maxSeq;
    }

    /**
     * 转换为Protobuf事件
     */
    private List<ImMessage> convertToProtoEvents(List<OfflineEventContent> eventContents) {
        return eventContents.stream().map(content -> {
            try {
                return ImMessage.parseFrom(content.getEventContent());
            } catch (InvalidProtocolBufferException e) {
                log.error("Failed to parse event content", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 清理过期事件
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupExpiredEvents() {
        log.info("Start cleaning up expired offline events");

        Date expireTime = new Date(System.currentTimeMillis() - 30 * 24 * 3600 * 1000L); // 30天前

        // 分批清理过期事件
        int batchSize = 1000;
        int totalDeleted = 0;
        int deleted;

        do {
            deleted = offlineEventMapper.deleteExpiredEvents(expireTime.getTime(), batchSize);
            totalDeleted += deleted;
            log.info("Deleted {} expired events in this batch", deleted);

            try {
                Thread.sleep(100); // 避免对数据库造成太大压力
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (deleted == batchSize);

        log.info("Finished cleaning up expired offline events, total deleted: {}", totalDeleted);
    }
}