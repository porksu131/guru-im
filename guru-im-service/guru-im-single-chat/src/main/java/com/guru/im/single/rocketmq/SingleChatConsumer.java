package com.guru.im.single.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.single.service.SingleChatService;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.SING_CHAT_TOPIC,
        selectorExpression = MQTag.CHAT,
        consumerGroup = "${rocketmq.consumers.chat-group}",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.ORDERLY)
public class SingleChatConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(SingleChatConsumer.class);
    @Autowired
    private SingleChatService singleChatService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.CHAT_MESSAGE) {
                singleChatService.processMessage(imMessage);
                return;
            }
            log.warn("can not process message, ignore it {}", imMessage.getBodyCase());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
