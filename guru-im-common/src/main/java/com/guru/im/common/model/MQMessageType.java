package com.guru.im.common.model;

public enum MQMessageType {
    UNKNOWN,
    CHAT,
    ACTION,
    REQUEST,
    NOTIFY,
    CONTROL,
    BATCH_SYNC,
    ACK,
    BATCH_ACK,
    SIGNAL
}
