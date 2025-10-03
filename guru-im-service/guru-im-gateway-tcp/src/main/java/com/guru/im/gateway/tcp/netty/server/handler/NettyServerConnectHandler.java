package com.guru.im.gateway.tcp.netty.server.handler;

import com.guru.im.core.common.event.NettyEvent;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.event.NettyEventType;
import com.guru.im.core.common.util.ChannelUtil;
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

    public NettyServerConnectHandler(NettyEventExecutor nettyEventExecutor) {
        this.nettyEventExecutor = nettyEventExecutor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.READ, remoteAddress, ctx));
        }
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
        LOGGER.debug("channelActive, the channel[{}]", remoteAddress);
        super.channelActive(ctx);
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.debug("channelInactive, the channel[{}]", remoteAddress);
        super.channelInactive(ctx);
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
                LOGGER.debug("IDLE exception [{}]", remoteAddress);
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

        ChannelUtil.closeChannel(ctx.channel());
        //ctx.fireExceptionCaught(cause);
    }
}
