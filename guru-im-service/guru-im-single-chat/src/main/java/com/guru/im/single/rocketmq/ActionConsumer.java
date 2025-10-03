package com.guru.im.single.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.single.service.ReadReceiptService;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.SING_CHAT_TOPIC,
        selectorExpression = MQTag.ACTION,
        consumerGroup = "${rocketmq.consumers.action-group}",
        messageModel = MessageModel.CLUSTERING)
public class ActionConsumer implements RocketMQListener<MQMessageWrapper> {

    @Autowired
    private ReadReceiptService readReceiptService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.READ_RECEIPT_REQ) {
                readReceiptService.processMessage(imMessage);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
