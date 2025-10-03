package com.guru.im.core.common.listener;

import com.guru.im.protocol.model.ImMessage;

import java.util.concurrent.CompletableFuture;

public class ResponseCallBack implements InvokeCallback{
    private final CompletableFuture<ResponseFuture> future;
    private final ResponseFuture responseFuture;

    public ResponseCallBack(CompletableFuture<ResponseFuture> future, ResponseFuture responseFuture) {
        this.future = future;
        this.responseFuture = responseFuture;
    }

    @Override
    public void operationSucceed(ImMessage response) {
        future.complete(responseFuture);
    }

    @Override
    public void operationFail(Throwable throwable) {
        future.completeExceptionally(throwable);
    }
}
