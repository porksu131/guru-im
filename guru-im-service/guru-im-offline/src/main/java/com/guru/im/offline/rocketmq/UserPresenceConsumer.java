package com.guru.im.offline.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.service.OfflineMessageService;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.PresenceNotify;
import com.guru.im.protocol.model.UserPresence;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
//@RocketMQMessageListener(topic = MQTopic.USR_TOPIC,
//        selectorExpression = MQTag.ACTION,
//        consumerGroup = "${rocketmq.consumers.common-group}",
//        messageModel = MessageModel.CLUSTERING)
public class UserPresenceConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger logger = LoggerFactory.getLogger(UserPresenceConsumer.class);
    @Autowired
    private OfflineMessageService offlineMessageService;

    @Override
    public void onMessage(MQMessageWrapper rocketMqMessage) {
        try {
            if (rocketMqMessage.getMessageType() == MQMessageType.ACTION) {
                ImMessage imMessage = ImMessage.parseFrom(rocketMqMessage.getBody());
                // 用户在线状态变更
                if (imMessage.getBodyCase() == ImMessage.BodyCase.PRESENCE_NOTIFY) {
                    PresenceNotify presenceNotify = imMessage.getPresenceNotify();
                    if (presenceNotify.getStatus() == UserPresence.ONLINE) {
                        offlineMessageService.pushOfflineMessage(presenceNotify);
                    }
                    return;
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