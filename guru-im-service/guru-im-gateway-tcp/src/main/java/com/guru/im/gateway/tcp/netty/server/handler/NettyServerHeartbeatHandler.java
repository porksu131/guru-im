package com.guru.im.gateway.tcp.netty.server.handler;

import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerHeartbeatHandler extends SimpleChannelInboundHandler<ImMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHeartbeatHandler.class);

    private final IMGatewayNettyServer nettyServer;

    public NettyServerHeartbeatHandler(IMGatewayNettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessage request) {
        if (request.getBodyCase() != ImMessage.BodyCase.HEARTBEAT_MESSAGE) {
            ctx.fireChannelRead(request);
            return;
        }
        ImMessage imMessage = nettyServer.getHeartbeatProcessor().processRequest(ctx, request);
        ctx.writeAndFlush(imMessage);
    }
}