package com.guru.im.demo.signal.handler;

import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(SignalingDispatcher.class);
    private final Map<SignalingType, SignalingHandler> handlers = new ConcurrentHashMap<>();
    
    public void registerHandler(SignalingHandler handler) {
        for (SignalingType type : handler.getSupportedTypes()) {
            handlers.put(type, handler);
            logger.info("注册信令处理器: {}", type);
        }
    }
    
    public void dispatch(SignalingMessage message) {
        SignalingHandler handler = handlers.get(message.getType());
        if (handler != null) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                logger.error("信令处理失败: type={}", message.getType(), e);
            }
        } else {
            logger.warn("未找到信令处理器: type={}", message.getType());
        }
    }
}