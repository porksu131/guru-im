package com.guru.im.mq.starter.core;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

public interface MQMessageSender {
    SendResult sendSync(String topic, MQMessageWrapper message);

    void sendAsync(String topic, MQMessageWrapper message, SendCallback sendCallback);

    void sendOneway(String topic, MQMessageWrapper message);

    SendResult sendOrderly(String topic, MQMessageWrapper message, String shardingKey);

    void sendOrderly(String topic, MQMessageWrapper message, String shardingKey, SendCallback sendCallback);

    LocalTransactionState sendTransactionMessage(String topic, MQMessageWrapper message);

    void sendWithRetry(String topic, MQMessageWrapper envelope);

    // topic从wrapper中取
    SendResult sendSync(MQMessageWrapper message);

    void sendAsync(MQMessageWrapper message, SendCallback sendCallback);

    void sendOneway(MQMessageWrapper message);

    SendResult sendOrderly(MQMessageWrapper message, String shardingKey);

    void sendOrderly(MQMessageWrapper message, String shardingKey, SendCallback sendCallback);

    LocalTransactionState sendTransactionMessage(MQMessageWrapper message);

    void sendWithRetry(MQMessageWrapper envelope);
}