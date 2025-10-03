package com.guru.im.mq.starter.core.exception;

public class MessageProcessingException extends RuntimeException {
    private final boolean shouldRetry;
    
    public MessageProcessingException(String message, boolean shouldRetry) {
        super(message);
        this.shouldRetry = shouldRetry;
    }

    public boolean isShouldRetry() {
        return shouldRetry;
    }
}
