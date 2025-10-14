package com.guru.im.core.im.handler;

import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private final IMClient client;

    public HeartbeatHandler(IMClient client) {
        this.client = client;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            client.sendHeartbeat();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ImMessage imMessage && imMessage.getBodyCase() == ImMessage.BodyCase.HEARTBEAT_MESSAGE) {
            // 心跳无需处理
        } else {
            // 不是心跳，下一个处理
            ctx.fireChannelRead(msg);
        }

    }

}