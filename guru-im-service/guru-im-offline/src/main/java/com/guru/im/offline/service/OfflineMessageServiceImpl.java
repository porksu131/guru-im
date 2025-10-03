package com.guru.im.offline.service;

import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.mapper.OfflineMessageContentMapper;
import com.guru.im.offline.mapper.OfflineMessageDeliveryMapper;
import com.guru.im.offline.model.pojo.OfflineMessageContent;
import com.guru.im.offline.model.pojo.OfflineMessageDelivery;
import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.FriendRequestNotify;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.PresenceNotify;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OfflineMessageServiceImpl implements OfflineMessageService {

    private static final Logger log = LoggerFactory.getLogger(OfflineMessageServiceImpl.class);
    private final OfflineMessageContentMapper contentMapper;
    private final OfflineMessageDeliveryMapper deliveryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SnowflakeIdGenerator idGenerator;

    private static final String OFFLINE_MSG_CACHE_KEY = "offline:msg:%s";
    private static final String OFFLINE_MSG_COUNT_CACHE_KEY = "offline:syncCount:%s:%s:%s";
    private static final String DELIVERY_STATUS_KEY = "delivery:status:%s:%s:%s";
    private static final long CACHE_EXPIRE_HOURS = 24;
    private final OfflineMessageDeliveryMapper offlineMessageDeliveryMapper;

    public OfflineMessageServiceImpl(OfflineMessageContentMapper contentMapper,
                                     OfflineMessageDeliveryMapper deliveryMapper,
                                     RedisTemplate<String, Object> redisTemplate,
                                     SnowflakeIdGenerator idGenerator,
                                     OfflineMessageDeliveryMapper offlineMessageDeliveryMapper) {
        this.contentMapper = contentMapper;
        this.deliveryMapper = deliveryMapper;
        this.redisTemplate = redisTemplate;
        this.idGenerator = idGenerator;
        this.offlineMessageDeliveryMapper = offlineMessageDeliveryMapper;
    }


    @Override
    public void saveOfflineChatMessage(MQMessageWrapper messageWrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(messageWrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.CHAT_MESSAGE) {
                saveChatMessage(messageWrapper, imMessage.getChatMessage());
                return;
            }
            log.error("unsupported im message type: {}", imMessage.getBodyCase());
        } catch (Exception e) {
            log.error("Save offline message failed", e);
            throw new RuntimeException("Save offline message failed", e);
        }
    }

    public void saveChatMessage(MQMessageWrapper messageWrapper, ChatMessage chatMessage) {
        try {
            OfflineMessageContent content = createOfflineChatMessageContent(messageWrapper, chatMessage);
            List<String> deviceIds = messageWrapper.getOfflineDeviceIds();

            // 保存消息内容
            contentMapper.insert(content);

            // 保存投递记录
            List<OfflineMessageDelivery> deliveries = deviceIds.stream()
                    .map(deviceId -> createDeliveryRecord(content.getMessageId(), 1, // 聊天消息
                            content.getReceiverId(), deviceId))
                    .collect(Collectors.toList());

            deliveryMapper.batchInsert(deliveries);

            // 更新缓存
            updateChatMessageCache(content, deliveries);
            log.info("save offline chat message for user: {}", content.getReceiverId());
        } catch (Exception e) {
            log.error("Save offline chat message failed", e);
            throw new RuntimeException("Save offline chat message failed", e);
        }
    }


    @Transactional
    @Override
    public void processSyncAck(Long userId, String deviceId, List<Long> messageIds) {
        try {
            // 更新投递状态为已投递
            deliveryMapper.batchUpdateDeliveryStatus(deviceId, messageIds, 1, System.currentTimeMillis()); // 1:已投递

            // 2. 检查哪些消息可以被安全删除
            List<Long> messagesToDelete = findMessagesSafeToDelete(userId, messageIds);

            // 3. 删除真正可以安全删除的消息
            if (!messagesToDelete.isEmpty()) {
                deleteMessagesSafe(messagesToDelete);
                log.info("Deleted {} safe messages for userId: {}", messagesToDelete.size(), userId);
            }

            log.info("Processed sync ack: userId={}, deviceId={}, messageCount={}",
                    userId, deviceId, messageIds.size());

        } catch (Exception e) {
            log.error("Process sync ack failed", e);
            throw new RuntimeException("Process sync ack failed", e);
        }
    }


    private OfflineMessageContent createOfflineChatMessageContent(MQMessageWrapper wrapper, ChatMessage chatMessage) {
        long now = System.currentTimeMillis();
        OfflineMessageContent content = new OfflineMessageContent();
        content.setMessageId(chatMessage.getMessageId());
        content.setSenderId(chatMessage.getSenderId());
        content.setReceiverId(wrapper.getReceiverIds().get(0));
        content.setConversationType(chatMessage.getConversationTypeValue());
        content.setConversationId(chatMessage.getConversationId());
        content.setMessageSeq(chatMessage.getServerSeq());
        content.setMessageType(chatMessage.getChatMessageTypeValue());
        content.setMessageContent(wrapper.getBody()); // 直接使用二进制，避免重复序列化
        content.setPriority(wrapper.getPriority());
        content.setMessageTime(chatMessage.getTimestamp());
        content.setExpireTime(null);
        content.setIsArchived(0);
        content.setArchiveTime(null);
        content.setCreateTime(now);
        content.setUpdateTime(now);
        return content;
    }

    private OfflineMessageDelivery createDeliveryRecord(Long messageId, int messageType, Long userId, String deviceId) {
        OfflineMessageDelivery delivery = new OfflineMessageDelivery();
        delivery.setId(idGenerator.nextId());
        delivery.setMessageId(messageId);
        delivery.setMessageType(messageType); // 1:聊天，2:好友请求
        delivery.setUserId(userId);
        delivery.setDeviceId(deviceId);
        delivery.setDeliveryStatus(0); // 未投递
        delivery.setDeliveryCount(0);
        delivery.setCreateTime(System.currentTimeMillis());
        delivery.setUpdateTime(System.currentTimeMillis());
        return delivery;
    }

    private void updateChatMessageCache(OfflineMessageContent content, List<OfflineMessageDelivery> deliveries) {
        String cacheKey = String.format(OFFLINE_MSG_CACHE_KEY, content.getReceiverId());
        redisTemplate.opsForZSet().add(cacheKey, content.getMessageId(), content.getMessageSeq());
        redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        // 更新投递状态缓存
        updateDeliveryCache(deliveries);
    }


    private void updateDeliveryCache(List<OfflineMessageDelivery> deliveries) {
        // 更新投递状态缓存
        deliveries.forEach(delivery -> {
            String statusKey = String.format(DELIVERY_STATUS_KEY, delivery.getUserId(),
                    delivery.getDeviceId(), delivery.getMessageType());
            redisTemplate.opsForHash().put(statusKey, delivery.getMessageId(), delivery.getDeliveryStatus());
        });
    }


    @Override
    public List<OfflineMessageContent> getOfflineMessages(Long userId, Integer conversationType, Long conversationId, Long lastSyncSeq, Integer limit) {
        Long currentTime = System.currentTimeMillis();
        return contentMapper.findByUserAndConversation(
                userId, conversationType, conversationId, lastSyncSeq, limit, currentTime);
    }

    /**
     * 检查哪些消息可以被安全删除
     */
    private List<Long> findMessagesSafeToDelete(Long userId, List<Long> messageIds) {
        List<Long> safeToDelete = new ArrayList<>();

        for (Long messageId : messageIds) {
            // 检查这个消息是否已经被所有设备确认
            if (isMessageDeliveredToAllDevices(userId, messageId)) {
                safeToDelete.add(messageId);
            }
        }

        return safeToDelete;
    }

    /**
     * 检查消息是否已被所有设备确认
     */
    private boolean isMessageDeliveredToAllDevices(Long userId, Long messageId) {
        try {
            // 1. 获取这个消息的所有投递记录
            List<OfflineMessageDelivery> deliveries =
                    deliveryMapper.findByMessageIdAndUser(userId, messageId);

            if (deliveries.isEmpty()) {
                return false;
            }

            // 2. 检查是否所有设备都已确认
            for (OfflineMessageDelivery delivery : deliveries) {
                if (delivery.getDeliveryStatus() != 1) { // 1: 已投递
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Check message delivery status failed: messageId={}", messageId, e);
            return false;
        }
    }

    /**
     * 安全删除消息（确保所有设备都已确认）
     */
    @Transactional
    public void deleteMessagesSafe(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return;
        }

        try {
            // 1. 先删除投递记录
            deliveryMapper.deleteByMessageIds(messageIds);

            // 2. 再删除消息内容
            contentMapper.deleteMessages(messageIds);

            log.info("Safely deleted {} messages", messageIds.size());

        } catch (Exception e) {
            log.error("Safe delete messages failed", e);
            throw new RuntimeException("Safe delete messages failed", e);
        }
    }

    /**
     * 归档已确认但还有其他设备未确认的消息
     */
    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点执行
    public void archiveOldMessages() {
        try {
            Long currentTime = System.currentTimeMillis();
            Long archiveTime = currentTime - (7 * 24 * 60 * 60 * 1000); // 7天前

            // 查找可以归档的旧消息
            List<Long> messagesToArchive = contentMapper.findMessagesToArchive(archiveTime);

            if (!messagesToArchive.isEmpty()) {
                // 归档消息
                contentMapper.archiveMessages(messagesToArchive, currentTime);
                log.info("Archived {} old messages", messagesToArchive.size());
            }

        } catch (Exception e) {
            log.error("Archive old messages failed", e);
        }
    }

    @Override
    public void updateDeliveryStatus(Long messageId, Long userId, String deviceId, Integer status) {
        Long updateTime = System.currentTimeMillis();
        deliveryMapper.updateDeliveryStatus(messageId, userId, deviceId, status, updateTime);

        // 更新缓存
        String statusKey = String.format(DELIVERY_STATUS_KEY, userId, deviceId);
        redisTemplate.opsForHash().put(statusKey, messageId, status);
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredMessages() {
        Long currentTime = System.currentTimeMillis();
        int deletedCount = contentMapper.deleteExpiredMessages(currentTime);
        log.info("Cleaned {} expired offline messages", deletedCount);
    }

    @Override
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanDeliveredMessages(Long userId) {
        int deletedCount = contentMapper.deleteDeliveredMessages(userId);
        log.info("Cleaned {} delivered messages for userId: {}", deletedCount, userId);
    }

    @Override
    public OfflineMessageContent getMessageById(Long messageId) {
        return contentMapper.findByMessageId(messageId);
    }

    @Override
    public Integer getUndeliveredMessageCount(Long userId) {
        return deliveryMapper.countUndeliveredByUser(userId);
    }

    @Override
    public List<OfflineMessageDelivery> getUndeliveredMessages(Long userId, Integer maxRetryCount, Integer limit) {
        return deliveryMapper.findUndeliveredMessages(userId, maxRetryCount, limit);
    }

    @Override
    public void retryDelivery(Long deliveryId) {
        OfflineMessageDelivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery != null) {
            Long currentTime = System.currentTimeMillis();
            deliveryMapper.updateDeliveryCount(deliveryId, delivery.getDeliveryCount() + 1, currentTime);

            // 重新触发投递逻辑
            triggerRedelivery(delivery);
        }
    }

    private void triggerRedelivery(OfflineMessageDelivery delivery) {
        // 实现重新投递的逻辑
        log.info("Retrying delivery for message: {}, user: {}, device: {}",
                delivery.getMessageId(), delivery.getUserId(), delivery.getDeviceId());
    }

    @Override
    public void archiveMessage(Long messageId) {
        Long archiveTime = System.currentTimeMillis();
        contentMapper.updateArchiveStatus(messageId, 1, archiveTime);
    }


    @Override
    public Integer countByUserAndConversation(Long userId, Integer conversationType,
                                              Long conversationId, Long lastSyncSeq) {
        try {
            Long currentTime = System.currentTimeMillis();

            // 先尝试从缓存获取
            //String cacheKey = String.format(OFFLINE_MSG_COUNT_CACHE_KEY, userId, conversationId, lastSyncSeq);
            //Integer cachedCount = getSyncCountFromCache(cacheKey);

            //if (cachedCount != null) {
                //return cachedCount;
            //}

            // 缓存中没有，查询数据库
            Integer count = contentMapper.countByUserAndConversation(
                    userId, conversationType, conversationId, lastSyncSeq);

            if (count == null) {
                count = 0;
            }

            // 缓存结果（短期缓存，1分钟）
            // redisTemplate.opsForValue().set(cacheKey, count, 1, TimeUnit.MINUTES);

            return count;

        } catch (Exception e) {
            log.error("Count messages failed: userId={}, conversationId={}", userId, conversationId, e);
            return 0; // 出错时返回0，避免影响正常流程
        }
    }

    private Integer getSyncCountFromCache(String cacheKey) {
        if (redisTemplate.hasKey(cacheKey)) {
            return (Integer) redisTemplate.opsForValue().get(cacheKey);
        }
        return null;
    }

    @Override
    public void pushOfflineMessage(PresenceNotify presenceNotify) {
        Long userId = presenceNotify.getUserId();
        String deviceId = presenceNotify.getDevice().getDeviceId();
        List<OfflineMessageDelivery> offlineMessageDeliveries = offlineMessageDeliveryMapper.selectUndeliveredByUserAndDevice(userId, deviceId, null);
        if (CollectionUtils.isEmpty(offlineMessageDeliveries)) {
            return;
        }
        //offlineMessageContentMapper.
        offlineMessageDeliveries.forEach(offlineMessageDelivery -> {
            // 1:聊天，2:好友请求
            if (offlineMessageDelivery.getMessageType() == 2) {

            }
        });
    }
}
