package com.guru.im.disaptch.rocketmq;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.disaptch.rocketmq.handler.base.IMMessageListener;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * messageModel=MessageModel.CLUSTERING
 * 监听模式，有消息就会消费
 */
@Service
@RocketMQMessageListener(topic = MQTopic.DISPATCH_ACTION_TOPIC,
        consumerGroup = "${rocketmq.consumers.action-group}",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING)
public class ActionMessageConsumer implements RocketMQListener<MQMessageWrapper> {
    private final IMMessageListener messageListener;

    public ActionMessageConsumer(IMMessageListener messageListener) {
        this.messageListener = messageListener;
    }


    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        messageListener.handleEgressMessage(wrapper);
    }
}
