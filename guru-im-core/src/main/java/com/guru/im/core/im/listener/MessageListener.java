package com.guru.im.core.im.listener;

import com.guru.im.protocol.model.ImMessage;

public interface MessageListener {
    void onOnewayMessage(ImMessage request);

    ImMessage onMessage(ImMessage request);

    default void onError(Throwable cause) {
    }
}