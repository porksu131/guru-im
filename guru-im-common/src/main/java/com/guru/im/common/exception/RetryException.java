package com.guru.im.common.exception;

/**
 * 重试异常
 */
public final class RetryException extends RuntimeException {
    public RetryException(String message) {
        super(message, null);
    }

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}