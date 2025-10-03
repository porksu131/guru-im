package com.guru.im.disaptch.rocketmq;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.retry.DistributedRetryService;
import com.guru.im.mq.starter.core.retry.RetryMessage;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.RETRY_TOPIC,
        consumerGroup = "${rocketmq.consumers.retry-group}",
        selectorExpression = "retry",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING)
public class RetryMessageConsumer implements RocketMQListener<RetryMessage> {
    @Autowired
    private DistributedRetryService retryService;

    @Override
    public void onMessage(RetryMessage retryMessage) {
        //retryService.handleRetryMessage(retryMessage);
    }
}
