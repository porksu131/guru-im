package com.guru.im.mq.starter.core.exception;

public class NoHandlerFoundException extends Exception {
    public NoHandlerFoundException(String message) {
        super(message, null);
    }

    public NoHandlerFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
