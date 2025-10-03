package com.guru.im.mq.starter.core.exception;

public class TemporaryFailureException extends MessageProcessingException {
    public TemporaryFailureException(String message) {
        super(message, true); // 临时故障需要重试
    }
}