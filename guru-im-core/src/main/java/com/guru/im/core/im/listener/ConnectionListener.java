package com.guru.im.core.im.listener;

public interface ConnectionListener {
    void onConnected();

    void onDisconnected();

    void onConnectFailed(String error);

    default void onReconnecting(int attemptCount) {
    }
}