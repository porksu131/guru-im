package com.guru.im.core.common.exception;

public class NettyConnectException extends Exception {
    public NettyConnectException(String message) {
        super(message);
    }

    public NettyConnectException(String addr, long timeoutMillis) {
        this(addr, timeoutMillis, null);
    }

    public NettyConnectException(String addr, long timeoutMillis, Throwable cause) {
        super("connect exception on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
}
