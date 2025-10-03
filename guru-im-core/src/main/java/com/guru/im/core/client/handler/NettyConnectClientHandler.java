package com.guru.im.core.client.handler;

import com.guru.im.core.common.event.NettyEvent;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.event.NettyEventType;
import com.guru.im.core.common.util.ChannelUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@ChannelHandler.Sharable
public class NettyConnectClientHandler extends ChannelDuplexHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(NettyConnectClientHandler.class);
    private final NettyEventExecutor nettyEventExecutor;

    public NettyConnectClientHandler(NettyEventExecutor nettyEventExecutor) {
        this.nettyEventExecutor = nettyEventExecutor;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        final String local = ChannelUtil.parseSocketAddressAddr(localAddress);
        final String remote = remoteAddress == null ? "UNKNOWN" : ChannelUtil.parseSocketAddressAddr(remoteAddress);
        LOGGER.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);

        super.connect(ctx, remoteAddress, localAddress, promise);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remote, ctx));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("NETTY CLIENT PIPELINE: unregistered, {}, channelId={}", remoteAddress, ctx.channel().id());
        super.channelUnregistered(ctx);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.UNREGISTERED, remoteAddress, ctx));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("NETTY CLIENT PIPELINE: ACTIVE, {}, channelId={}", remoteAddress, ctx.channel().id());
        super.channelActive(ctx);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.ACTIVE, remoteAddress, ctx));
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
        super.disconnect(ctx, promise);
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx));
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("NETTY CLIENT PIPELINE: CLOSE channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
        super.close(ctx, promise);
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.info("NETTY CLIENT PIPELINE: channelInactive, the channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
                LOGGER.warn("NETTY CLIENT PIPELINE: IDLE exception channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
                if (nettyEventExecutor.getChannelEventListeners() != null) {
                    nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx));
                }
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.warn("NETTY CLIENT PIPELINE: exceptionCaught channel[addr={}, id={}]", remoteAddress, ctx.channel().id(), cause);
        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx));
        }
    }
}
