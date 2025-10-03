package com.guru.im.core.common.listener;

public interface MessageSendCallback {
    default void onSuccess() {
    }

    default void onFailure(String error) {
    }
}