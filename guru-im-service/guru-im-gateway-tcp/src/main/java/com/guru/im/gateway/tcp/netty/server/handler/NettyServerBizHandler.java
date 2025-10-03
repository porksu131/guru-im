package com.guru.im.gateway.tcp.netty.server.handler;


import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class NettyServerBizHandler extends SimpleChannelInboundHandler<ImMessage> {
    private final IMGatewayNettyServer nettyServer;

    public NettyServerBizHandler(IMGatewayNettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessage msg) {
        this.nettyServer.getMessageProcessManager().processMessageReceived(ctx, msg);
    }
}
