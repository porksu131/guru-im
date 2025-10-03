package com.guru.im.user.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.GroupInviteNotify;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.user.rocketmq.handler.FriendNotifyService;
import com.guru.im.user.rocketmq.handler.GroupInviteNotifyService;
import com.guru.im.user.rocketmq.handler.UserConnectionService;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.USR_TOPIC,
        selectorExpression = MQTag.ACK,
        consumerGroup = "${rocketmq.consumers.ack-group}",
        messageModel = MessageModel.CLUSTERING)
public class AckConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger logger = LoggerFactory.getLogger(UserPresenceConsumer.class);

    @Autowired
    private FriendNotifyService friendNotifyService;
    @Autowired
    private GroupInviteNotifyService groupInviteNotifyService;

    @Override
    public void onMessage(MQMessageWrapper rocketMqMessage) {
        try {
            if (rocketMqMessage.getMessageType() == MQMessageType.ACK) {
                ImMessage imMessage = ImMessage.parseFrom(rocketMqMessage.getBody());
                if (CorrelationType.FRIEND_REQUEST_NOTIFY == rocketMqMessage.getCorrelationType()) {
                    // 好友关系变更ack
                    friendNotifyService.processFriendRelationChange(rocketMqMessage.getCorrelationId(), imMessage.getResponse());
                    logger.info("process friend request notify ack completed");
                } else if (CorrelationType.GROUP_INVITE_NOTIFY == rocketMqMessage.getCorrelationType()) {
                    // 群聊通知ack
                    groupInviteNotifyService.processGroupInviteAck(rocketMqMessage.getCorrelationId(), imMessage.getResponse());
                    logger.info("process group invite notify ack completed");
                }
            }

        } catch (InvalidProtocolBufferException e) {
            logger.error("parse im message error:{}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
