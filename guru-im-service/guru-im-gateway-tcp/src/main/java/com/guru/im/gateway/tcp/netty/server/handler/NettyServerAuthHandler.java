package com.guru.im.gateway.tcp.netty.server.handler;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.core.manager.MessageProcessManager;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerAuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerAuthHandler.class);

    private final IMGatewayNettyServer nettyServer;

    public NettyServerAuthHandler(IMGatewayNettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ImMessage request)) {
            ctx.fireChannelRead(msg);
            return;
        }
        MessageProcessManager processManager = this.nettyServer.getMessageProcessManager();
        try {
            ImMessage imResponse = nettyServer.getAuthDispatchProcessor().processRequest(ctx, request);
            Response response = imResponse.getResponse();
            if (ResponseCode.SUCCESS == response.getCode()) {
                ctx.pipeline().remove(this); // 认证成功后，移除当前处理器，后续信息不再经过此处理器
            }
            processManager.writeResponse(ctx.channel(), request, imResponse); // 结果返回即可，不再向后传递
        } catch (Exception e) {
            LOGGER.error("AuthHandler error:{}", e.getMessage(), e);
            processManager.writeResponseErr(ctx.channel(), request, ResponseCode.SYSTEM_ERROR, e.getMessage());
            ctx.close(); // 认证失败，关闭连接
        }
    }
}