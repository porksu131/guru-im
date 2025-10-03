package com.guru.im.mq.starter.core.exception;

public class BusinessValidationException extends MessageProcessingException {
    public BusinessValidationException(String message) {
        super(message, false); // 业务验证失败不重试
    }
}