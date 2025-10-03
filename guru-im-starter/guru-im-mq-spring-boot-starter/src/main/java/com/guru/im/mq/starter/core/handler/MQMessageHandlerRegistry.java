package com.guru.im.mq.starter.core.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MQMessageHandlerRegistry {
    private final Map<String, MQMessageHandler> handlerMap = new ConcurrentHashMap<>();
    
    public void register(String key, MQMessageHandler handler) {
        handlerMap.put(key, handler);
    }
    
    public MQMessageHandler getHandler(String key) {
        return handlerMap.get(key);
    }
}