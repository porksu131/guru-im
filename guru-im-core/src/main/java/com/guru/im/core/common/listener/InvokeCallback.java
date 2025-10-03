package com.guru.im.core.common.listener;

import com.guru.im.protocol.model.ImMessage;

public interface InvokeCallback {
    default void operationComplete(final ResponseFuture responseFuture) {

    }

    default void operationSucceed(final ImMessage imMessage) {

    }

    default void operationFail(final Throwable throwable) {

    }
}
