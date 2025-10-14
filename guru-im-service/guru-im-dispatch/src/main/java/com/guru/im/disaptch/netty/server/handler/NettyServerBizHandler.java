package com.guru.im.disaptch.netty.server.handler;

import com.guru.im.disaptch.IMDispatchNettyServer;
import com.guru.im.disaptch.netty.server.ChannelAttributeUtils;
import com.guru.im.protocol.model.GatewayInfo;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class NettyServerBizHandler extends SimpleChannelInboundHandler<ImMessage> {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerBizHandler.class);
    private final IMDispatchNettyServer nettyServer;

    public NettyServerBizHandler(IMDispatchNettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessage msg) {
        if (msg.getBodyCase() == ImMessage.BodyCase.GATEWAY_INFO) {
            GatewayInfo gatewayInfo = msg.getGatewayInfo();
            nettyServer.getChannelCacheManager().saveChannel(gatewayInfo.getGatewayServerAddr(), ctx.channel());
            ChannelAttributeUtils.setGatewayInfo(ctx.channel(), gatewayInfo);
            logger.info("SAVE NEW GATEWAY ADDRESS CHANNEL: {}", gatewayInfo.getGatewayServerAddr());
        } else if (msg.getBodyCase() == ImMessage.BodyCase.HEARTBEAT_MESSAGE) {
            // do nothing
        } else {
            this.nettyServer.getMessageProcessManager().processMessageReceived(ctx, msg);
        }
    }
}
