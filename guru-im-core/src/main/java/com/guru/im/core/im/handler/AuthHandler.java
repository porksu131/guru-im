package com.guru.im.core.im.handler;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.im.exception.AuthException;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.System;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private final String token;
    private final DeviceInfo deviceInfo;
    private boolean authenticated = false;

    public AuthHandler(String token, DeviceInfo deviceInfo) {
        this.token = token;
        this.deviceInfo = deviceInfo;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 发送认证消息
        AuthMessage authMessage = AuthMessage.newBuilder()
                .setToken(token)
                .setAuthTime(System.currentTimeMillis())
                .setAuthType(AuthType.AUTH_LOGIN)
                .setDeviceInfo(deviceInfo)
                .build();
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.AUTH)
                .setAuthMessage(authMessage)
                .build();
        ctx.writeAndFlush(imMessage).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("send auth failed");
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ImMessage imMessage
                && imMessage.getBodyCase() == ImMessage.BodyCase.RESPONSE
                && imMessage.getMessageType() == MessageType.AUTH) {
            Response response = imMessage.getResponse();
            if (ResponseCode.SUCCESS != response.getCode()) {
                throw new AuthException(response.getMsg());
            }

            authenticated = true;
            ctx.pipeline().remove(this);
        } else if (!authenticated) {
            throw new AuthException("Authentication required");
        } else {
            super.channelRead(ctx, msg);
        }
    }
}