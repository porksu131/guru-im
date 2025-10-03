package com.guru.im.core.common.processor;

import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;

public interface MessageProcessor {
    ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) throws Exception;

    void processOneway(ChannelHandlerContext ctx, ImMessage request) throws Exception;
}
