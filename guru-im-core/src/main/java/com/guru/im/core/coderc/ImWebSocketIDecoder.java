package com.guru.im.core.coderc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class ImWebSocketIDecoder extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        try {
            // 剥离WebSocket帧头，保留原始TCP数据
            ctx.fireChannelRead(frame.content().retain());
        } finally {
            // 确保释放WebSocket帧，防止内存泄漏
            frame.release();
        }
    }
}
