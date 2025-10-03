package com.guru.im.mq.starter.core.retry;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// 分布式重试服务
public class DistributedRetryService {

    private static final Logger log = LoggerFactory.getLogger(DistributedRetryService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final RetryStrategyExecutor retryExecutor;

    private static final String RETRY_QUEUE = "retry-topic";
    private static final String DLQ_QUEUE = "dlq-topic";

    public DistributedRetryService(RedisTemplate<String, Object> redisTemplate,
                                   RocketMQTemplate rocketMQTemplate,
                                   RetryStrategyExecutor retryExecutor) {
        this.redisTemplate = redisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.retryExecutor = retryExecutor;
    }

    /**
     * 安排重试
     */
    public void scheduleRetry(MQMessageWrapper envelope, String reason) {
        RetryConfig config = retryExecutor.getConfig(envelope.getMessageType());

        RetryMessage retryMessage = RetryMessage.builder()
                .messageId(envelope.getMessageId())
                .originalMessage(envelope)
                .attemptCount(1)
                .nextRetryTime(System.currentTimeMillis() +
                        retryExecutor.calculateDelay(config, 1))
                .retryReason(reason)
                .messageType(envelope.getMessageType())
                .originalTopic(envelope.getTargetTopic())
                .originalTag(envelope.getTargetTag())
                .build();

        // 存储到Redis
        redisTemplate.opsForValue().set(
                retryMessage.getRedisKey(),
                retryMessage,
                24, TimeUnit.HOURS); // TTL 24小时

        // 发送到重试队列
        rocketMQTemplate.sendOneWay(RETRY_QUEUE + ":retry", retryMessage);
    }

    /**
     * 处理重试消息
     */
    public void handleRetryMessage(RetryMessage retryMessage) {
        try {
            if (!retryMessage.shouldRetry()) {
                // 还没到重试时间，重新入队
                rocketMQTemplate.sendOneWay(RETRY_QUEUE + ":retry", retryMessage);
                return;
            }

            MQMessageWrapper envelope = retryMessage.getOriginalMessage();

            boolean success = retryExecutor.executeWithRetry(envelope, () -> {
                // 重新执行原始操作
                return sendOriginalMessage(envelope,
                        retryMessage.getOriginalTopic(),
                        retryMessage.getOriginalTag());
            });

            if (success) {
                // 重试成功，清理
                redisTemplate.delete(retryMessage.getRedisKey());
            } else {
                // 重试失败，安排下一次重试或进入死信队列
                handleRetryFailure(retryMessage);
            }

        } catch (Exception e) {
            log.error("Failed to process retry message: {}", retryMessage.getMessageId(), e);
        }
    }

    public boolean executeWithRetry(MQMessageWrapper envelope, Supplier<Boolean> operation){
        return retryExecutor.executeWithRetry(envelope, operation);
    }

    private Boolean sendOriginalMessage(MQMessageWrapper envelope, String originalTopic, String originalTag) {
        SendResult sendResult = rocketMQTemplate.syncSend(originalTopic + ":" + originalTag, envelope);
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * 处理重试失败
     */
    private void handleRetryFailure(RetryMessage retryMessage) {
        retryMessage.setAttemptCount(retryMessage.getAttemptCount() + 1);

        RetryConfig config = retryExecutor.getConfig(retryMessage.getMessageType());
        if (retryMessage.getAttemptCount() >= config.getMaxAttempts()) {
            // 达到最大重试次数，进入死信队列
            moveToDLQ(retryMessage);
        } else {
            // 安排下一次重试
            long nextDelay = retryExecutor.calculateDelay(config, retryMessage.getAttemptCount());
            retryMessage.setNextRetryTime(System.currentTimeMillis() + nextDelay);

            redisTemplate.opsForValue().set(
                    retryMessage.getRedisKey(), retryMessage, 24, TimeUnit.HOURS);

            rocketMQTemplate.sendOneWay(RETRY_QUEUE + ":retry", retryMessage);
        }
    }

    /**
     * 移动到死信队列
     */
    private void moveToDLQ(RetryMessage retryMessage) {
        log.warn("Moving message to DLQ: {}", retryMessage.getMessageId());
        rocketMQTemplate.sendOneWay(DLQ_QUEUE + ":dlq", retryMessage);
        redisTemplate.delete(retryMessage.getRedisKey());
    }
}