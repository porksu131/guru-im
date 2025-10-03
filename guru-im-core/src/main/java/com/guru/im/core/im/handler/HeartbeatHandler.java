package com.guru.im.core.im.handler;

import com.guru.im.core.im.IMClient;
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
}