package com.guru.im.gateway.tcp.netty.client.dispatch;

import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;

public interface GatewayClientDispatcher {
    ImMessage forwardMessageToUser(ChannelHandlerContext ctx, ImMessage request);

    void forwardOnewayToUser(ChannelHandlerContext ctx, ImMessage request);
}
