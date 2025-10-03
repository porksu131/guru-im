package com.guru.im.single.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.constant.*;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.ConversationType;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageContent;
import com.guru.im.protocol.util.MessageParserHelper;
import com.guru.im.single.mapper.ConversationMapper;
import com.guru.im.single.mapper.GroupMapper;
import com.guru.im.single.mapper.MessageMapper;
import com.guru.im.single.mapper.UserConversationMapper;
import com.guru.im.single.model.pojo.Conversation;
import com.guru.im.single.model.pojo.Message;
import com.guru.im.single.model.pojo.UserConversation;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SingleChatService {

    private static final Logger log = LoggerFactory.getLogger(SingleChatService.class);
    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final UserConversationMapper userConversationMapper;
    private final GroupMapper groupMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RetryPushService retryPushService;

    public SingleChatService(MessageMapper messageMapper,
                             ConversationMapper conversationMapper,
                             UserConversationMapper userConversationMapper,
                             GroupMapper groupMapper,
                             SnowflakeIdGenerator idGenerator,
                             RetryPushService retryPushService) {
        this.messageMapper = messageMapper;
        this.conversationMapper = conversationMapper;
        this.userConversationMapper = userConversationMapper;
        this.groupMapper = groupMapper;
        this.idGenerator = idGenerator;
        this.retryPushService = retryPushService;
    }

    /**
     * 处理单聊消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(ImMessage imMessage) {
        ChatMessage chatMessage = imMessage.getChatMessage();
        try {
            // 1. 验证会话是否存在
            if (!validateConversationExists(chatMessage.getConversationId())) {
                log.error("会话不存在，丢弃消息: messageId={}", chatMessage.getMessageId());
                return;
            }
            // 2. 解析消息内容
            MessageContent messageContent = MessageParserHelper.parseChatMessageContent(chatMessage);

            // 3. 转换为数据库实体
            Message messageEntity = convertToEntity(chatMessage, messageContent);

            // 4. 插入消息
            messageMapper.insert(messageEntity);

            // 5. 更新会话最后一条消息信息
            updateConversationInfo(chatMessage);

            List<Long> receiverIds = getReceiverIds(chatMessage);

            // 6. 更新接收方未读计数
            updateUnreadCount(chatMessage, receiverIds);

            // 7. 推送消息到分发层
            asyncPushToDispatch(imMessage, receiverIds);

            log.debug("单聊消息处理成功: messageId={}", imMessage.getMsgId());

        } catch (DuplicateKeyException e) {
            log.warn("重复消息已忽略: clientMsgId={}", chatMessage.getClientMsgId());
        } catch (Exception e) {
            log.error("处理单聊消息失败: messageId={}", imMessage.getMsgId(), e);
            throw new RuntimeException("消息处理失败", e);
        }
    }

    private List<Long> getReceiverIds(ChatMessage chatMessage) {
        if (chatMessage.getConversationType() == ConversationType.PRIVATE) {
            return Collections.singletonList(chatMessage.getReceiverId());
        }
        List<Long> groupMembers = groupMapper.selectGroupMembers(chatMessage.getReceiverId());
        if (CollectionUtils.isEmpty(groupMembers)) {
            log.error("groupId={}, no groupMembers", chatMessage.getReceiverId());
            return Collections.emptyList();
        }
        groupMembers.remove(chatMessage.getSenderId());
        return groupMembers;
    }

    /**
     * 异步推送消息到分发层（支持重试）
     */
    private void asyncPushToDispatch(ImMessage imMessage, List<Long> receiverIds) {
        ChatMessage chatMessage = imMessage.getChatMessage();
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setCorrelationType(CorrelationType.CHAT_MESSAGE);
        wrapper.setCorrelationId(String.valueOf(imMessage.getChatMessage().getMessageId()));
        wrapper.setSourceType(SourceType.SINGLE_CHAT_SERVICE);
        wrapper.setMessageType(MQMessageType.CHAT);
        wrapper.setReplyTopic(MQTopic.SING_CHAT_TOPIC);
        wrapper.setReplyTag(MQTag.ACK);
        wrapper.setTargetTopic(MQTopic.DISPATCH_CHAT_TOPIC);
        wrapper.setReceiverIds(receiverIds);
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setBody(imMessage.toByteArray());
        retryPushService.AsyncPushToDispatch(wrapper, new RetryPushListener() {
            @Override
            public void onPushSuccess(int attempt) {
                log.info("push message to mq success, messageId={}, attempt={}", attempt, chatMessage.getMessageId());
                updateDeliveryStatusForRetry(chatMessage.getMessageId(), DeliveryStatus.DELIVERED_WAIT_ARRIVE, attempt);
            }

            @Override
            public void onPushFailed(int attempt) {
                log.error("push message to mq failed, messageId={}, attempt={}", attempt, chatMessage.getMessageId());
                updateDeliveryStatusForRetry(chatMessage.getMessageId(), DeliveryStatus.DELIVERY_FAILED, attempt);
            }
        });
    }

    /**
     * 验证会话是否存在
     */
    private boolean validateConversationExists(Long conversationId) {
        Integer count = conversationMapper.countByConversationId(conversationId);
        return count != null && count > 0;
    }

    private Message convertToEntity(ChatMessage chatMessage, MessageContent messageContent) {
        Message entity = new Message();

        // 设置基础字段
        entity.setId(chatMessage.getMessageId());
        entity.setClientMsgId(chatMessage.getClientMsgId());
        entity.setClientSendTime(chatMessage.getClientSendTime());
        entity.setConversationType(chatMessage.getConversationTypeValue());
        entity.setConversationId(chatMessage.getConversationId());
        entity.setSenderId(chatMessage.getSenderId());
        entity.setReceiverId(chatMessage.getReceiverId());
        entity.setMessageType(chatMessage.getChatMessageTypeValue());

        // 序列化消息内容为JSON
        try {
            String jsonContent = objectMapper.writeValueAsString(messageContent.getContent());
            entity.setMessageContent(jsonContent);
        } catch (JsonProcessingException e) {
            log.error("消息内容序列化失败", e);
            throw new RuntimeException("消息内容序列化失败");
        }

        // 设置其他字段
        entity.setServerSeq(chatMessage.getServerSeq());
        entity.setClientSeq(chatMessage.getClientSeq());
        entity.setAtUsers(serializeAtUsers(chatMessage.getAtUsersList()));
        entity.setRecalled(false);
        entity.setReadCount(0);
        entity.setStatus(1);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setUpdateTime(System.currentTimeMillis());

        return entity;
    }

    private void updateConversationInfo(ChatMessage chatMessage) {
        // 使用乐观锁更新会话信息
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            Long currentVersion = conversationMapper.selectVersionById(chatMessage.getConversationId());
            if (currentVersion == null) {
                log.error("会话不存在，无法更新最后消息: conversationId={}", chatMessage.getConversationId());
                return;
            }

            int updated = conversationMapper.updateLastMessageById(
                    chatMessage.getConversationId(),
                    chatMessage.getMessageId(),
                    chatMessage.getTimestamp(),
                    MessageParserHelper.exactMessageSummary(chatMessage),
                    chatMessage.getSenderId(),
                    chatMessage.getServerSeq(),
                    System.currentTimeMillis(),
                    currentVersion
            );

            if (updated > 0) return;

            log.debug("会话更新乐观锁冲突，重试: conversationId={}", chatMessage.getConversationId());
        }

        log.error("更新会话信息失败，已达到最大重试次数: conversationId={}", chatMessage.getConversationId());
    }

    private String serializeAtUsers(List<Long> atUsers) {
        if (atUsers == null || atUsers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(atUsers);
        } catch (JsonProcessingException e) {
            log.warn("@用户列表序列化失败", e);
            return null;
        }
    }

    /**
     * 更新未读计数
     */
    private void updateUnreadCount(ChatMessage chatMessage, List<Long> receiverIds) {
        for (Long receiverId : receiverIds) {
            updatePrivateUnreadCount(receiverId, chatMessage);
        }
    }

    /**
     * 更新用户会话的未读计数
     */
    private void updatePrivateUnreadCount(Long receiverId, ChatMessage chatMessage) {
        long conversationId = chatMessage.getConversationId();
        long updateTime = System.currentTimeMillis();

        try {
            // 先尝试更新现有记录
            int updatedRows = userConversationMapper.incrementUnreadCount(
                    receiverId, conversationId, 1, updateTime
            );

            if (updatedRows == 0) {
                log.warn("用户会话关系不存在，可能数据不一致: userId={}, conversationId={}", receiverId, conversationId);
            }

        } catch (Exception e) {
            log.error("更新未读计数失败: userId={}, conversationId={}",
                    receiverId, conversationId, e);
            throw new RuntimeException("更新未读计数失败", e);
        }
    }


    /**
     * 更新推送状态
     */
    public void updateDeliveryStatus(long messageId, DeliveryStatus status) {
        long currentTime = System.currentTimeMillis();
        int updates = messageMapper.updateDeliveryStatus(
                messageId,
                status.getCode(),
                status == DeliveryStatus.ARRIVED ? currentTime : null,
                currentTime
        );
    }

    /**
     * 更新推送状态
     */
    private void updateDeliveryStatusForRetry(long messageId, DeliveryStatus status, int retryCount) {
        long currentTime = System.currentTimeMillis();
        int updates = messageMapper.updateDeliveryStatusForRetry(
                messageId,
                status.getCode(),
                status == DeliveryStatus.ARRIVED ? currentTime : null,
                retryCount,
                retryCount == 0 ? currentTime : null,
                currentTime
        );
    }

}
