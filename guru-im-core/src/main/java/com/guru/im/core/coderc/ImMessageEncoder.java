package com.guru.im.core.coderc;

import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.protocol.model.ImMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImMessageEncoder extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImMessageEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            if (msg instanceof ImMessage) {
                ByteBuf outByteBuf = ctx.alloc().buffer();
                // 可以根据需要选择不同的序列化方式
                ImCustomerSerialize.encode((ImMessage) msg, outByteBuf);
                ctx.write(outByteBuf, promise);
            } else {
                ctx.write(msg, promise);
            }
        } catch (Exception e) {
            LOGGER.error("encode exception, {}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), e);
            promise.setFailure(e);
            ReferenceCountUtil.release(msg);
        }
    }
}