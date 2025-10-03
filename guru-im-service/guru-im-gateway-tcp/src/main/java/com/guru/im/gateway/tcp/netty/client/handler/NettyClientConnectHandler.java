package com.guru.im.gateway.tcp.netty.client.handler;

import com.guru.im.core.common.event.NettyEvent;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.event.NettyEventType;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.gateway.tcp.IMGatewayNettyClient;
import com.guru.im.gateway.tcp.netty.server.ChannelAttributeUtils;
import com.guru.im.protocol.model.GatewayInfo;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class NettyClientConnectHandler extends ChannelDuplexHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(NettyClientConnectHandler.class);
    private final NettyEventExecutor nettyEventExecutor;
    private final IMGatewayNettyClient imGatewayNettyClient;

    public NettyClientConnectHandler(NettyEventExecutor nettyEventExecutor,
                                     IMGatewayNettyClient imGatewayNettyClient) {
        this.nettyEventExecutor = nettyEventExecutor;
        this.imGatewayNettyClient = imGatewayNettyClient;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        final String remote = remoteAddress == null ? "UNKNOWN" : ChannelUtil.parseSocketAddressAddr(remoteAddress);
        super.connect(ctx, remoteAddress, localAddress, promise);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remote, ctx));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        super.channelUnregistered(ctx);

        if (nettyEventExecutor.getChannelEventListeners() != null) {
            nettyEventExecutor.putNettyEvent(new NettyEvent(NettyEventType.UNREGISTERED, remoteAddress, ctx));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        final String localHost = ChannelUtil.getLocalHost();

        String gatewayServerAddr = localHost + ":" + imGatewayNettyClient.getLocalServerPort();
        GatewayInfo gatewayInfo = GatewayInfo.newBuilder().setGatewayServerAddr(gatewayServerAddr).build();
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.ONEWAY)
                .setGatewayInfo(gatewayInfo)
                .build();

        ctx.writeAndFlush(imMessage);

        ChannelAttributeUtils.setGatewayInfo(ctx.channel(), gatewayInfo);

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

                ImMessage heartbeat = MessageBuilder.createHeartbeat();
                this.imGatewayNettyClient.sendOneway(ctx.channel(), heartbeat, 5000);

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
