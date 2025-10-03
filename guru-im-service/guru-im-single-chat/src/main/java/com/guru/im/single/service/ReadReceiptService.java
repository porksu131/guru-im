package com.guru.im.single.service;

import com.guru.im.cache.starter.distribute.id.SequenceIdGenerator;
import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.DeliveryStatus;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ConversationType;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.ReadReceiptNotify;
import com.guru.im.protocol.model.ReadReceiptReq;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.single.mapper.ConversationReadMapper;
import com.guru.im.single.mapper.MessageMapper;
import com.guru.im.single.model.dto.ConversationKey;
import com.guru.im.single.model.pojo.ConversationRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReadReceiptService {
    private static final Logger log = LoggerFactory.getLogger(ReadReceiptService.class);
    private final ConversationReadMapper conversationReadMapper;
    private final MessageMapper messageMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final SequenceIdGenerator sequenceIdGenerator;
    private final RetryPushService retryPushService;

    public ReadReceiptService(ConversationReadMapper conversationReadMapper,
                              MessageMapper messageMapper,
                              SnowflakeIdGenerator idGenerator,
                              SequenceIdGenerator sequenceIdGenerator,
                              RetryPushService retryPushService) {
        this.conversationReadMapper = conversationReadMapper;
        this.messageMapper = messageMapper;
        this.idGenerator = idGenerator;
        this.sequenceIdGenerator = sequenceIdGenerator;
        this.retryPushService = retryPushService;
    }

    /**
     * 处理已读回执上报
     */
    @Transactional
    public void processMessage(ImMessage imMessage) {
        Long now = System.currentTimeMillis();
        ReadReceiptReq req = imMessage.getReadReceiptReq();
        Long userId = req.getReadId();

        // 获取当前的已读位置
        ConversationRead current = conversationReadMapper.selectByUserAndConversation(
                userId, req.getConversationTypeValue(), req.getConversationId());
        long currentReadSeq = current != null ? current.getLastReadSeq() : 0L;
        long nextId = current == null ? idGenerator.nextId() : current.getId();
        Long globalSeq = sequenceIdGenerator.nextGlobalSeq();

        // 更新会话已读指针（仅当新位置更大时）
        if (req.getLastReadSeq() > currentReadSeq) {
            updateConversationRead(nextId, globalSeq, userId, req, now);

            // 异步更新消息已读计数
            asyncUpdateReadCount(userId, req, currentReadSeq, now);

            // 通知相关方
            notifyReadReceipt(nextId, globalSeq, userId, req, now);
        }
    }

    private void updateConversationRead(long nextId, Long globalSeq, Long userId, ReadReceiptReq req, Long now) {
        ConversationRead conversationRead = new ConversationRead();
        conversationRead.setId(nextId);
        conversationRead.setGlobalSeq(globalSeq);
        conversationRead.setUserId(userId);
        conversationRead.setConversationType(req.getConversationType().getNumber());
        conversationRead.setConversationId(req.getConversationId());
        conversationRead.setLastReadSeq(req.getLastReadSeq());
        conversationRead.setCreateTime(now);
        conversationRead.setUpdateTime(now);

        int count = conversationReadMapper.upsertReadPosition(conversationRead);
    }

    private void notifyReadReceipt(long nextId, Long globalSeq, Long userId, ReadReceiptReq req, Long now) {
        ReadReceiptNotify notify = ReadReceiptNotify.newBuilder()
                .setConversationId(req.getConversationId())
                .setConversationType(req.getConversationType())
                .setLastReadSeq(req.getLastReadSeq())
                .setReadId(userId)
                .setReadTime(now)
                .setGlobalSeq(globalSeq)
                .build();
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setReadReceiptNotify(notify)
                .build();


        // 私聊通知发送方，群聊通知所有成员
        if (req.getConversationType() == ConversationType.PRIVATE) {
            // 私聊：需要查询消息发送方
            Long senderId = getSenderId(req);
            if (senderId == null) {
                log.warn("notifyReadReceipt failed senderId is null");
               return;
            }
            sendToUser(senderId, imMessage, nextId, globalSeq);
        } else {
            // 群聊：通知所有在线群成员
            //sendToGroup(req.getConversationId(), imMessage, nextId);
        }
    }

    /**
     * 推送消息到分发层
     */
    private void sendToUser(Long senderId, ImMessage imMessage, long nextId, Long globalSeq) {
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setGlobalSeq(globalSeq);
        wrapper.setMessageType(MQMessageType.ACTION);
        wrapper.setSourceType(SourceType.SINGLE_CHAT_SERVICE);
        wrapper.setTargetTopic(MQTopic.DISPATCH_ACTION_TOPIC);
        wrapper.setReplyTopic(MQTopic.SING_CHAT_TOPIC);
        wrapper.setReceiverIds(Collections.singletonList(senderId));
        wrapper.setCorrelationType(CorrelationType.READ_RECEIPT_NOTIFY);
        wrapper.setCorrelationId(String.valueOf(nextId));
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setBody(imMessage.toByteArray());
        retryPushService.AsyncPushToDispatch(wrapper, new RetryPushListener() {
            @Override
            public void onPushSuccess(int attempt) {
                updateDeliveryStatus(nextId, DeliveryStatus.DELIVERED_WAIT_ARRIVE);
            }

            @Override
            public void onPushFailed(int attempt) {
                updateDeliveryStatus(nextId, DeliveryStatus.DELIVERY_FAILED);
            }
        });
    }


    // 更新推送状态
    public void updateDeliveryStatus(Long id, DeliveryStatus deliveryStatus) {
        Long now = System.currentTimeMillis();
        conversationReadMapper.updateDeliveryStatus(
                id,
                deliveryStatus.getCode(),
                DeliveryStatus.ARRIVED == deliveryStatus ? now : null,
                now);
    }


    private Long getSenderId(ReadReceiptReq req) {
        return messageMapper.selectRecentSenderExcludeUser(req.getConversationTypeValue(),
                req.getConversationId(), req.getReadId());
    }

    /**
     * 获取用户所有会话的已读状态（用于多设备同步）
     */
    public List<ConversationRead> getUserReadPositions(Long userId) {
        return conversationReadMapper.selectByUserId(userId);
    }

    /**
     * 获取用户在会话中的已读位置
     */
    public Long getUserReadPosition(Long userId, Integer conversationType, Long conversationId) {
        ConversationRead read = conversationReadMapper.selectByUserAndConversation(
                userId, conversationType, conversationId);
        return read != null ? read.getLastReadSeq() : 0L;
    }

    /**
     * 计算会话的未读消息数
     */
    public int getUnreadCount(Long userId, Integer conversationType, Long conversationId) {
        Long lastReadSeq = getUserReadPosition(userId, conversationType, conversationId);
        Long maxSeq = messageMapper.getMaxServerSeq(conversationType, conversationId);
        return (int) (maxSeq - lastReadSeq);
    }

    /**
     * 批量获取会话的未读消息数
     */
    public Map<Long, Integer> batchGetUnreadCount(Long userId, List<ConversationKey> conversations) {
        Map<Long, Integer> result = new HashMap<>();

        // 批量查询已读位置
        List<ConversationRead> readPositions = conversationReadMapper.selectByUserAndConversations(userId, conversations);
        Map<String, Long> positionMap = readPositions.stream()
                .collect(Collectors.toMap(
                        read -> read.getConversationType() + "_" + read.getConversationId(),
                        ConversationRead::getLastReadSeq
                ));

        // 计算每个会话的未读数
        for (ConversationKey conv : conversations) {
            String key = conv.getConversationType() + "_" + conv.getConversationId();
            Long lastReadSeq = positionMap.getOrDefault(key, 0L);
            Long maxSeq = messageMapper.getMaxServerSeq(conv.getConversationType(), conv.getConversationId());
            result.put(conv.getConversationId(), (int) (maxSeq - lastReadSeq));
        }

        return result;
    }

    /**
     * 获取当前的已读位置
     */
    private Long getCurrentReadSeq(Long userId, ReadReceiptReq req) {
        ConversationRead current = conversationReadMapper.selectByUserAndConversation(
                userId, req.getConversationType().getNumber(), req.getConversationId());
        return current != null ? current.getLastReadSeq() : 0L;
    }

    /**
     * 异步更新消息已读计数
     */
    @Async("taskExecutor")
    public void asyncUpdateReadCount(Long userId, ReadReceiptReq req, Long currentReadSeq, Long updateTime) {
        try {
            if (req.getLastReadSeq() <= currentReadSeq) {
                return; // 没有新的已读消息
            }

            Long startSeq = currentReadSeq + 1;
            Long endSeq = req.getLastReadSeq();
            long size = endSeq - startSeq;

            if (size < 1000) {
                // 直接更新（适合消息量不大的情况）
                updateReadCountDirect(userId, req, startSeq, endSeq, updateTime);
            } else {
                // 分批更新（适合消息量大的情况）
                updateReadCountInBatches(userId, req, startSeq, endSeq, updateTime, 1000);
            }

            log.info("Updated read count for user {} in conversation {}, seq range: {}-{}",
                    userId, req.getConversationId(), startSeq, endSeq);

        } catch (Exception e) {
            log.error("Failed to async update read count for user: {}, conversation: {}",
                    userId, req.getConversationId(), e);
        }
    }

    /**
     * 直接更新已读计数
     */
    private void updateReadCountDirect(Long userId, ReadReceiptReq req, Long startSeq, Long endSeq, Long updateTime) {
        int affectedRows = messageMapper.incrementReadCountBySeqRange(
                req.getConversationType().getNumber(),
                req.getConversationId(),
                userId,
                startSeq,
                endSeq,
                updateTime
        );

        log.debug("Direct update affected {} rows for seq range {}-{}", affectedRows, startSeq, endSeq);
    }

    /**
     * 分批更新已读计数（避免大事务）
     */
    private void updateReadCountInBatches(Long userId, ReadReceiptReq req, Long startSeq,
                                          Long endSeq, Long updateTime, int batchSize) {
        long currentStart = startSeq;

        while (currentStart <= endSeq) {
            long currentEnd = Math.min(currentStart + batchSize - 1, endSeq);

            // 获取本批次需要更新的消息ID
            List<Long> messageIds = messageMapper.selectUnreadMessagesBySeq(
                    req.getConversationType().getNumber(),
                    req.getConversationId(),
                    userId,
                    currentStart,
                    currentEnd
            );

            if (!messageIds.isEmpty()) {
                // 批量更新已读计数
                int updates = messageMapper.batchIncrementReadCount(messageIds, updateTime);
                log.debug("Batch update {} messages for seq range {}-{}",
                        updates, currentStart, currentEnd);
            }

            currentStart = currentEnd + 1;

            // 稍微休息一下，避免数据库压力过大
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

}