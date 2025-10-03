package com.guru.im.disaptch.rocketmq.retry;

public interface RetryResultHandler {
    void handleRetrySuccess(DispatchRetryContext dispatchRetryContext);

    void handleRetryFailed(DispatchRetryContext dispatchRetryContext);
}
