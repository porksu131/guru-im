package com.guru.im.mq.starter.core.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;

public interface MQMessageHandler {
    /**
     * 处理消息
     */
    void handle(MQMessageWrapper wrapper) throws InvalidProtocolBufferException;
}