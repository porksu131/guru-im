package com.guru.im.core.coderc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public class ImWebSocketIEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            // 将ByteBuf包装为WebSocket帧
            if (msg instanceof ByteBuf) {
                ctx.write(new BinaryWebSocketFrame((ByteBuf) msg), promise);
            } else {
                // 其他类型直接转发（如异常消息）
                ctx.write(msg, promise);
            }
        } catch (Exception e) {
            promise.setFailure(e);
            ReferenceCountUtil.release(msg);
        }
    }
}
