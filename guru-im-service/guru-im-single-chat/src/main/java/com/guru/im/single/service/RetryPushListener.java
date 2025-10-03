package com.guru.im.single.service;

public interface RetryPushListener {
    void onPushSuccess(int attempt);

    void onPushFailed(int attempt);
}