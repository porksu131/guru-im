package com.guru.im.core.common.exception;

public class SendTimeoutException extends Exception {
    public SendTimeoutException(String message) {
        super(message);
    }

    public SendTimeoutException(String addr, long timeoutMillis) {
        this(addr, timeoutMillis, null);
    }

    public SendTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super("wait response on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
}
