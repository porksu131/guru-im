package com.guru.im.offline.service;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.model.pojo.OfflineMessageContent;
import com.guru.im.offline.model.pojo.OfflineMessageDelivery;
import com.guru.im.protocol.model.PresenceNotify;

import java.util.List;

public interface OfflineMessageService {

    void saveOfflineChatMessage(MQMessageWrapper messageWrapper);

    List<OfflineMessageContent> getOfflineMessages(Long userId, Integer conversationType,
                                                   Long conversationId, Long lastSyncSeq, Integer limit);

    void processSyncAck(Long userId, String deviceId, List<Long> messageIds);

    void updateDeliveryStatus(Long messageId, Long userId, String deviceId, Integer status);

    void cleanExpiredMessages();

    void cleanDeliveredMessages(Long userId);

    OfflineMessageContent getMessageById(Long messageId);

    Integer getUndeliveredMessageCount(Long userId);

    List<OfflineMessageDelivery> getUndeliveredMessages(Long userId, Integer maxRetryCount, Integer limit);

    void retryDelivery(Long deliveryId);

    void archiveMessage(Long messageId);

    Integer countByUserAndConversation(Long userId, Integer conversationType, Long conversationId, Long lastSyncSeq);

    void pushOfflineMessage(PresenceNotify presenceNotify);
}