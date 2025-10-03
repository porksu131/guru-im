package com.guru.im.disaptch.rocketmq.handler.base;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MessageHandlerDispatcher {
    private static final Logger log = LoggerFactory.getLogger(MessageHandlerDispatcher.class);
    private final List<MessageHandler> messageHandlers;

    public MessageHandlerDispatcher(List<MessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    /**
     * 分发消息到合适的处理器
     */
    public void dispatch(MQMessageWrapper envelope) {
        try {
            findHandler(envelope).ifPresentOrElse(
                handler -> handler.handle(envelope),
                () -> handleNoHandlerFound(envelope)
            );
        } catch (Exception e) {
            log.error("Failed to dispatch message: {}", envelope.getMessageId(), e);
            handleDispatchFailure(envelope, e);
        }
    }

    /**
     * 查找合适的处理器
     */
    private Optional<MessageHandler> findHandler(MQMessageWrapper envelope) {
        return messageHandlers.stream()
                .filter(handler -> handler.canHandle(envelope))
                .findFirst();
    }

    /**
     * 处理找不到处理器的情况
     */
    private void handleNoHandlerFound(MQMessageWrapper envelope) {
        log.warn("No handler found for message: type={}, topic={}, id={}", 
                envelope.getMessageType(), envelope.getTargetTopic(), envelope.getMessageId());
        
        // 可以发送到死信队列供后续分析
        // sendToDLQ(envelope);
    }

    /**
     * 处理分发失败
     */
    private void handleDispatchFailure(MQMessageWrapper envelope, Exception e) {
        log.error("Message dispatch failed, sending to retry queue: {}", envelope.getMessageId());
        // 可以在这里实现重试逻辑
        // sendToRetryQueue(envelope);
    }
}