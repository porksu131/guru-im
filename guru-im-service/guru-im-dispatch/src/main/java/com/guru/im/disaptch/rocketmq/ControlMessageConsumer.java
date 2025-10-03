package com.guru.im.disaptch.rocketmq;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.disaptch.rocketmq.handler.base.IMMessageListener;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.DISPATCH_CONTROL_TOPIC,
        consumerGroup = "${rocketmq.consumers.control-group}",
        consumeThreadNumber = 4,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING)
public class ControlMessageConsumer implements RocketMQListener<MQMessageWrapper> {
    private final IMMessageListener messageListener;

    public ControlMessageConsumer(IMMessageListener messageListener) {
        this.messageListener = messageListener;
    }


    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        messageListener.handleControlMessage(wrapper);
    }
}
