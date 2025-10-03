package com.guru.im.gateway.tcp.netty.server.dispatch;

import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;

@Service
public interface GatewayServerDispatcher {
    ImMessage forwardMessageToDispatch(ChannelHandlerContext ctx, ImMessage request);

    void forwardOnewayToDispatch(ChannelHandlerContext ctx, ImMessage request);
}
