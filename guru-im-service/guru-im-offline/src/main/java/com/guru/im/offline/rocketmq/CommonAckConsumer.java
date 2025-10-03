package com.guru.im.offline.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.service.CommonAckService;
import com.guru.im.protocol.model.ImMessage;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.OFFLINE_TOPIC,
        selectorExpression = MQTag.ACK,
        consumerGroup = "${rocketmq.consumers.ack-group}",
        messageModel = MessageModel.CLUSTERING)
public class CommonAckConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(CommonAckConsumer.class);
    @Autowired
    private CommonAckService commonAckService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            if (wrapper.getMessageType() == MQMessageType.ACK) {
                ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
                commonAckService.processMessage(wrapper, imMessage);
                return;
            }
            log.warn("can not process message, ignore it {}", wrapper.getMessageType());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}