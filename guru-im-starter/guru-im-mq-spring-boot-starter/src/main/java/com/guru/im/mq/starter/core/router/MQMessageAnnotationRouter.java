package com.guru.im.mq.starter.core.router;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.mq.starter.core.exception.NoHandlerFoundException;
import com.guru.im.mq.starter.core.handler.MQMessageHandler;
import com.guru.im.mq.starter.core.handler.MQMessageHandlerRegistry;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;

public class MQMessageAnnotationRouter implements MQMessageRouter {
    private final MQMessageHandlerRegistry registry;

    public MQMessageAnnotationRouter(MQMessageHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void route(MQMessageWrapper wrapper) throws NoHandlerFoundException, InvalidProtocolBufferException {
        String handlerKey = wrapper.getHandlerKey();
        MQMessageHandler handler = registry.getHandler(handlerKey);
        if (handler == null) {
            throw new NoHandlerFoundException("handlerKey [" + handlerKey + "] not found");
        }
        handler.handle(wrapper);
    }
}