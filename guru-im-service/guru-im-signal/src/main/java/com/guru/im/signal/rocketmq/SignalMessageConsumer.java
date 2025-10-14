package com.guru.im.signal.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.signal.service.SignalingMessageHandler;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.SIGNAL_TOPIC,
        selectorExpression = MQTag.SIGNAL,
        consumerGroup = "${rocketmq.consumers.signal}",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.ORDERLY)
public class SignalMessageConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(SignalMessageConsumer.class);
    @Autowired
    private SignalingMessageHandler signalHandler;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.SIGNALING_MESSAGE) {
                signalHandler.handleSignalingMessage(imMessage.getSignalingMessage());
                return;
            }
            log.warn("can not process message, ignore it {}", imMessage.getBodyCase());
        } catch (InvalidProtocolBufferException e) {
            log.error("处理信令消息失败: {}", wrapper.getMessageId(), e);
            throw new RuntimeException(e);
        }
    }
}