package com.guru.im.sdk.event;

import com.guru.im.protocol.model.ImMessage;

public class IMEvent {
    private final IMEventType type;
    private final ImMessage imMessage;
    private final Object data;

    public IMEvent(IMEventType type, ImMessage imMessage, Object data) {
        this.type = type;
        this.imMessage = imMessage;
        this.data = data;
    }

    public IMEventType getType() {
        return type;
    }

    public ImMessage getImMessage() {
        return imMessage;
    }

    public Object getData() {
        return data;
    }
}