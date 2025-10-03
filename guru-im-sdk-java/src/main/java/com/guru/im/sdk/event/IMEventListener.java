package com.guru.im.sdk.event;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;

public interface IMEventListener {
    boolean canHandle(IMEvent event);

    void onEvent(IMEvent event);

    default boolean isCustomerResponse() {
        return false;
    }

    default ImMessage Response(IMEvent event) {
        // 默认收到消息自动响应成功，可重写实现自定义响应
        return MessageBuilder.createImResponse(event.getImMessage(), ResponseCode.SUCCESS, "message received");
    }
}