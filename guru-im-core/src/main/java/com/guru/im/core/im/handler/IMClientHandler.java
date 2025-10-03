package com.guru.im.core.im.handler;

import com.guru.im.core.im.state.ConnectionState;
import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.TimeUnit;

public class IMClientHandler extends SimpleChannelInboundHandler<ImMessage> {
    private final IMClient client;

    public IMClientHandler(IMClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessage msg) throws Exception {
        this.client.getMessageProcessManager().processMessageReceived(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (client.getMessageListener() != null) {
            client.getMessageListener().onError(cause);
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (client.getState() != ConnectionState.DISCONNECTED) {
            ctx.channel().eventLoop().schedule(() -> {
                if (!client.isForceOffline()) {
                    client.getConnectionListener().onDisconnected();
                }
            }, 3, TimeUnit.SECONDS); // 3秒后触发重连，避免先触发重连后强制下线，所以留给强制下线处理时间

        }
        super.channelInactive(ctx);
    }
}