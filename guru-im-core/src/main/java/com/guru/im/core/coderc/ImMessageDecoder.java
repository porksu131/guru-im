package com.guru.im.core.coderc;

import com.guru.im.core.common.util.ChannelUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImMessageDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImMessageDecoder.class);
    private static final int FRAME_MAX_LENGTH = 16777216;

    public ImMessageDecoder() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 0);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }
            return ImCustomerSerialize.decode(frame);
        } catch (Exception e) {
            LOGGER.error("decode exception, {}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), e);
            //ChannelUtil.closeChannel(ctx.channel());
            throw e;
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
    }
}