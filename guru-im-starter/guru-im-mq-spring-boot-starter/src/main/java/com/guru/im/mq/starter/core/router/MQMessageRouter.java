package com.guru.im.mq.starter.core.router;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.mq.starter.core.exception.NoHandlerFoundException;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;

public interface MQMessageRouter {
    /**
     * 路由消息到合适的处理器
     */
    void route(MQMessageWrapper wrapper) throws NoHandlerFoundException, InvalidProtocolBufferException;
}