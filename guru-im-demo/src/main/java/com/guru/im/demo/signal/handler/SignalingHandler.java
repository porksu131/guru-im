package com.guru.im.demo.signal.handler;

import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;

import java.util.Set;

public interface SignalingHandler {
    /**
     * 支持的信号类型
     */
    Set<SignalingType> getSupportedTypes();
    
    /**
     * 处理信令消息
     */
    void handle(SignalingMessage message);
}