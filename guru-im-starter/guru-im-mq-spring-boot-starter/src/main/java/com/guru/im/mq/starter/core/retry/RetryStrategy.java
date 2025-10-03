package com.guru.im.mq.starter.core.retry;

// 重试策略类型
public enum RetryStrategy {
    IMMEDIATE,          // 立即重试
    FIXED_DELAY,        // 固定延迟
    EXPONENTIAL_BACKOFF,// 指数退避
    RANDOM_DELAY,       // 随机延迟
    CUSTOM              // 自定义
}