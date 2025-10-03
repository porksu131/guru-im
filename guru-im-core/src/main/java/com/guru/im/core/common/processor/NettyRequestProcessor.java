package com.guru.im.core.common.processor;

import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Common remoting command processor
 */
public interface NettyRequestProcessor {
    int bizType();

    boolean rejectProcess(ImMessage request);

    ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) throws Exception;
}
