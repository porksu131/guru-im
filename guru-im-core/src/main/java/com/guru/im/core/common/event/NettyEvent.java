package com.guru.im.core.common.event;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class NettyEvent {
    private final NettyEventType type;
    private final String remoteAddr;
    private final ChannelHandlerContext ctx;

    public NettyEvent(NettyEventType type, String remoteAddr, ChannelHandlerContext ctx) {
        this.type = type;
        this.remoteAddr = remoteAddr;
        this.ctx = ctx;
    }

    public NettyEventType getType() {
        return type;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @Override
    public String toString() {
        return "NettyEvent [type=" + type + ", remoteAddr=" + remoteAddr + ", channel=" + ctx + "]";
    }
}
