package com.guru.im.disaptch.netty.server.handler;

import com.guru.im.core.common.event.NettyEvent;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.event.NettyEventType;
import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.disaptch.IMDispatchNettyServer;
import com.guru.im.disaptch.netty.server.ChannelAttributeUtils;
import com.guru.im.protocol.model.GatewayInfo;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class NettyServerConnectHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerConnectHandler.class);
    private final NettyEventExecutor nettyEventExecutor;

    private final IMDispatchNettyServer nettyServer;

    public NettyServerConnectHandler(NettyEventExecutor nettyEventExecutor, IMDispatchNettyServer nettyServer) {
        this.nettyEventExecutor = nettyEventExecutor;
        this.nettyServer = nettyServer;
    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.debug("channelRegistered {}", remoteAddress);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.debug("channelUnregistered, the channel[{}]", remoteAddress);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("channelActive, the channel[{}]", remoteAddress);
        super.channelActive(ctx);
        //final String remoteHost = ChannelUtil.parseChannelRemoteHost(ctx.channel());
        //nettyServer.getChannelCacheManager().saveChannel(remoteAddress, ctx.channel());
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("channelInactive, the channel[{}]", remoteAddress);
        super.channelInactive(ctx);
        GatewayInfo gatewayInfo = ChannelAttributeUtils.getGatewayInfo(ctx.channel());
        nettyServer.getChannelCacheManager().removeChannel(gatewayInfo.getGatewayServerAddr(), ctx.channel());
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
                LOGGER.debug("IDLE exception [{}]", remoteAddress);
                ImMessage heartbeat = MessageBuilder.createHeartbeat();
                this.nettyServer.sendOneway(ctx.channel(), heartbeat, 5000);
                if (nettyEventExecutor.getChannelEventListeners() != null) {
                    nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx));
                }
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.warn("exceptionCaught {}", remoteAddress);
        LOGGER.warn("exceptionCaught exception.", cause);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx));
        }
        GatewayInfo gatewayInfo = ChannelAttributeUtils.getGatewayInfo(ctx.channel());
        nettyServer.getChannelCacheManager().removeChannel(gatewayInfo.getGatewayServerAddr(), ctx.channel());
        ChannelUtil.closeChannel(ctx.channel());
    }
}
