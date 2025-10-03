package com.guru.im.disaptch.rocketmq.handler.base;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IMMessageListener {

    private static final Logger log = LoggerFactory.getLogger(IMMessageListener.class);
    private final MessageHandlerDispatcher handlerDispatcher;

    public IMMessageListener(MessageHandlerDispatcher handlerDispatcher) {
        this.handlerDispatcher = handlerDispatcher;
    }

    /**
     * 处理上行消息（从网关到微服务）
     */
    public void handleIngressMessage(MQMessageWrapper envelope) {
        log.debug("Processing ingress message: {}", envelope.getMessageId());
        handlerDispatcher.dispatch(envelope);
    }

    /**
     * 处理下行消息（从微服务到用户）
     */
    public void handleEgressMessage(MQMessageWrapper envelope) {
        log.debug("Processing egress message: {}", envelope.getMessageId());
        handlerDispatcher.dispatch(envelope);
    }

    /**
     * 处理控制消息（从微服务到用户）
     */
    public void handleControlMessage(MQMessageWrapper envelope) {
        log.debug("Processing control message: {}", envelope.getMessageId());
        handlerDispatcher.dispatch(envelope);
    }
}