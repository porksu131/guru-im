package com.guru.im.core.client.handler;

import com.guru.im.core.client.BaseNettyClient;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<ImMessage> {
    private final BaseNettyClient nettyClient;;

    public NettyClientHandler(BaseNettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessage msg) throws Exception {
        this.nettyClient.getMessageProcessManager().processMessageReceived(ctx, msg);
    }
}
