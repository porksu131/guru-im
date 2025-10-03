package com.guru.im.core.common.event;

import io.netty.channel.ChannelHandlerContext;

public interface ChannelEventListener {
    void onChannelConnect(final String remoteAddr, final ChannelHandlerContext ctx);

    void onChannelClose(final String remoteAddr, final ChannelHandlerContext ctx);

    void onChannelException(final String remoteAddr, final ChannelHandlerContext ctx);

    void onChannelIdle(final String remoteAddr, final ChannelHandlerContext ctx);

    void onChannelActive(final String remoteAddr, final ChannelHandlerContext ctx);

    void onUnregistered(final String remoteAddr, final ChannelHandlerContext ctx);
}
