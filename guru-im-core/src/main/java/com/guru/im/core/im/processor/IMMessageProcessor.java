package com.guru.im.core.im.processor;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.core.im.listener.MessageListener;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelHandlerContext;

public class IMMessageProcessor implements MessageProcessor {
    private final MessageListener messageListener;

    public IMMessageProcessor(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    @Override
    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        if (messageListener != null) {
            return messageListener.onMessage(request);
        }
        return MessageBuilder.createImResponse(request, ResponseCode.SUCCESS, "message received");
    }

    @Override
    public void processOneway(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        if (messageListener != null) {
            messageListener.onOnewayMessage(request);
            return;
        }
        throw new RuntimeException("Message processing failed, no message listener available");
    }
}
