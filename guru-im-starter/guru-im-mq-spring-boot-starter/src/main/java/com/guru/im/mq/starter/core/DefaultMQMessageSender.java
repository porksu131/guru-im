package com.guru.im.mq.starter.core;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.mq.starter.core.retry.DistributedRetryService;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class DefaultMQMessageSender implements MQMessageSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMQMessageSender.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final DistributedRetryService retryService;

    public DefaultMQMessageSender(RocketMQTemplate rocketMQTemplate,
                                  DistributedRetryService retryService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.retryService = retryService;
    }

    @Override
    public SendResult sendSync(String topic, MQMessageWrapper message) {
        Message<MQMessageWrapper> msg = MessageBuilder.withPayload(message).build();
        return rocketMQTemplate.syncSend(topic, msg);
    }

    @Override
    public void sendAsync(String topic, MQMessageWrapper message, SendCallback sendCallback) {
        Message<MQMessageWrapper> msg = MessageBuilder.withPayload(message).build();
        rocketMQTemplate.asyncSend(topic, msg, sendCallback);
    }

    @Override
    public void sendOneway(String topic, MQMessageWrapper message) {
        Message<MQMessageWrapper> msg = MessageBuilder.withPayload(message).build();
        rocketMQTemplate.sendOneWay(topic, msg);
    }

    @Override
    public SendResult sendOrderly(String topic, MQMessageWrapper message, String shardingKey) {
        Message<MQMessageWrapper> msg = MessageBuilder.withPayload(message).build();
        return rocketMQTemplate.syncSendOrderly(topic, msg, shardingKey);
    }

    @Override
    public void sendOrderly(String topic, MQMessageWrapper message, String shardingKey, SendCallback sendCallback) {
        Message<MQMessageWrapper> msg = MessageBuilder.withPayload(message).build();
        rocketMQTemplate.asyncSendOrderly(topic, msg, shardingKey, sendCallback);
    }

    /**
     * 4.发送事务消息
     */
    @Override
    public LocalTransactionState sendTransactionMessage(String topic, MQMessageWrapper message) {
        Message<MQMessageWrapper> msg = MessageBuilder
                .withPayload(message)
                .build();
        // 发送事务消息
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(topic, msg, null);
        LOGGER.info("事务消息状态：{}", result.getLocalTransactionState());
        return result.getLocalTransactionState();
    }

    @Override
    public SendResult sendSync(MQMessageWrapper message) {
        return this.sendSync(message.getDestination(), message);
    }

    @Override
    public void sendAsync(MQMessageWrapper message, SendCallback sendCallback) {
        this.sendAsync(message.getDestination(), message, sendCallback);
    }

    @Override
    public void sendOneway(MQMessageWrapper message) {
        this.sendOneway(message.getDestination(), message);
    }

    @Override
    public SendResult sendOrderly(MQMessageWrapper message, String shardingKey) {
        return this.sendOrderly(message.getDestination(), message, shardingKey);
    }

    @Override
    public void sendOrderly(MQMessageWrapper message, String shardingKey, SendCallback sendCallback) {
        this.sendOrderly(message.getDestination(), message, shardingKey, sendCallback);
    }

    @Override
    public LocalTransactionState sendTransactionMessage(MQMessageWrapper message) {
        return this.sendTransactionMessage(message.getDestination(), message);
    }

    /**
     * 带重试的消息发送
     */
    @Override
    public void sendWithRetry(String topic, MQMessageWrapper envelope) {
        boolean success = retryService.executeWithRetry(envelope, () -> {
            try {
                SendResult result = this.sendSync(topic, envelope);
                return result.getSendStatus() == SendStatus.SEND_OK;
            } catch (Exception e) {
                LOGGER.warn("Message send failed: {}", envelope.getMessageId(), e);
                return false;
            }
        });

        if (!success) {
            retryService.scheduleRetry(envelope, "Send failed");
        }
    }

    @Override
    public void sendWithRetry(MQMessageWrapper envelope) {
        this.sendWithRetry(envelope.getDestination(), envelope);
    }
}
