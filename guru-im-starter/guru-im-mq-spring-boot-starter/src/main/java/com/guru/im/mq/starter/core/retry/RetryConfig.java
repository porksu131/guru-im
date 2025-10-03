package com.guru.im.mq.starter.core.retry;

import java.util.Random;

public class RetryConfig {
    private final RetryStrategy strategy;
    private final int maxAttempts;          // 最大重试次数
    private final long initialDelayMs;      // 初始延迟
    private final long maxDelayMs;          // 最大延迟
    private final double backoffFactor;     // 退避因子
    private final Random jitter;            // 随机抖动

    private RetryConfig(Builder builder) {
        strategy = builder.strategy;
        maxAttempts = builder.maxAttempts;
        initialDelayMs = builder.initialDelayMs;
        maxDelayMs = builder.maxDelayMs;
        backoffFactor = builder.backoffFactor;
        jitter = builder.jitter;
    }

    // 默认配置
    public static RetryConfig defaultConfig() {
        return RetryConfig.builder()
                .strategy(RetryStrategy.EXPONENTIAL_BACKOFF)
                .maxAttempts(5)
                .initialDelayMs(1000)
                .maxDelayMs(30000)
                .backoffFactor(2.0)
                .jitter(new Random())
                .build();
    }

    public static Builder builder() {
        return Builder.newBuilder();
    }


    public RetryStrategy getStrategy() {
        return strategy;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public double getBackoffFactor() {
        return backoffFactor;
    }

    public Random getJitter() {
        return jitter;
    }

    public static final class Builder {
        private RetryStrategy strategy;
        private int maxAttempts;
        private long initialDelayMs;
        private long maxDelayMs;
        private double backoffFactor;
        private Random jitter;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder strategy(RetryStrategy val) {
            strategy = val;
            return this;
        }

        public Builder maxAttempts(int val) {
            maxAttempts = val;
            return this;
        }

        public Builder initialDelayMs(long val) {
            initialDelayMs = val;
            return this;
        }

        public Builder maxDelayMs(long val) {
            maxDelayMs = val;
            return this;
        }

        public Builder backoffFactor(double val) {
            backoffFactor = val;
            return this;
        }

        public Builder jitter(Random val) {
            jitter = val;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}