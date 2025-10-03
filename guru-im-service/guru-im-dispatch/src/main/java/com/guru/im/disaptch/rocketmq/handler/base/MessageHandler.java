package com.guru.im.disaptch.rocketmq.handler.base;

import com.guru.im.mq.starter.core.message.MQMessageWrapper;

/**
 * 消息处理器接口
 */
public interface MessageHandler {

    /**
     * 判断该处理器是否能处理此消息
     */
    boolean canHandle(MQMessageWrapper wrapper);

    /**
     * 处理消息
     */
    void handle(MQMessageWrapper wrapper);
}