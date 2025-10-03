package com.guru.im.mq.starter.core.retry;

import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RetryStrategyExecutor {
    private static final Logger log = LoggerFactory.getLogger(RetryStrategyExecutor.class);

    // 不同消息类型的重试配置
    private final Map<MQMessageType, RetryConfig> retryConfigMap = new ConcurrentHashMap<>();

    public RetryStrategyExecutor() {
        initDefaultRetryConfig();
    }

    private void initDefaultRetryConfig() {
        retryConfigMap.put(MQMessageType.CONTROL, RetryConfig.builder()
                .strategy(RetryStrategy.IMMEDIATE)
                .maxAttempts(3)
                .initialDelayMs(100)
                .build());
        retryConfigMap.put(MQMessageType.CHAT, RetryConfig.builder()
                .strategy(RetryStrategy.EXPONENTIAL_BACKOFF)
                .maxAttempts(5)
                .initialDelayMs(1000)
                .maxDelayMs(10000)
                .backoffFactor(2.0)
                .build());
        retryConfigMap.put(MQMessageType.ACTION, RetryConfig.builder()
                .strategy(RetryStrategy.FIXED_DELAY)
                .maxAttempts(2)
                .initialDelayMs(500)
                .build());
    }

    /**
     * 执行重试
     */
    public boolean executeWithRetry(MQMessageWrapper envelope, Supplier<Boolean> operation) {
        RetryConfig config = retryConfigMap.getOrDefault(
                envelope.getMessageType(), RetryConfig.defaultConfig());

        return executeWithRetry(envelope, operation, config, 1);
    }

    private boolean executeWithRetry(MQMessageWrapper envelope, Supplier<Boolean> operation,
                                     RetryConfig config, int attempt) {
        try {
            boolean success = operation.get();
            if (success) {
                return true;
            }

            if (attempt >= config.getMaxAttempts()) {
                log.warn("Max retry attempts reached for message: {}", envelope.getMessageId());
                return false;
            }

            long delay = calculateDelay(config, attempt);
            log.debug("Retry attempt {} for message {}, delay {}ms",
                    attempt, envelope.getMessageId(), delay);

            Thread.sleep(delay);
            return executeWithRetry(envelope, operation, config, attempt + 1);

        } catch (Exception e) {
            log.error("Retry attempt {} failed for message: {}",
                    attempt, envelope.getMessageId(), e);

            if (attempt >= config.getMaxAttempts()) {
                return false;
            }

            long delay = calculateDelay(config, attempt);
            try {
                Thread.sleep(delay);
                return executeWithRetry(envelope, operation, config, attempt + 1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 计算重试延迟
     */
    public long calculateDelay(RetryConfig config, int attempt) {
        switch (config.getStrategy()) {
            case IMMEDIATE:
                return 0;

            case FIXED_DELAY:
                return config.getInitialDelayMs();

            case EXPONENTIAL_BACKOFF:
                long delay = (long) (config.getInitialDelayMs() *
                        Math.pow(config.getBackoffFactor(), attempt - 1));
                return Math.min(delay, config.getMaxDelayMs());

            case RANDOM_DELAY:
                return config.getInitialDelayMs() +
                        config.getJitter().nextInt(1000);

            default:
                return config.getInitialDelayMs();
        }
    }

    public void registerRetryConfig(MQMessageType mqMessageType, RetryConfig retryConfig) {
        this.retryConfigMap.put(mqMessageType, retryConfig);
    }

    public RetryConfig removeRetryConfig(MQMessageType mqMessageType) {
        return this.retryConfigMap.remove(mqMessageType);
    }

    public RetryConfig getConfig(MQMessageType messageType) {
        return retryConfigMap.get(messageType);
    }
}