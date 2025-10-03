package com.guru.im.core.common.exception;

public class SendRequestException extends Exception {
    public SendRequestException(String addr) {
        this(addr, null);
    }

    public SendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
