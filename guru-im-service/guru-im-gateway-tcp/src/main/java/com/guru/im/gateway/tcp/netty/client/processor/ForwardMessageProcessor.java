package com.guru.im.gateway.tcp.netty.client.processor;

import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.gateway.tcp.netty.client.dispatch.GatewayClientDispatcher;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("clientForwardMessageProcessor")
public class ForwardMessageProcessor implements MessageProcessor {

    @Autowired
    private GatewayClientDispatcher gatewayClientDispatcher;

    @Override
    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        return gatewayClientDispatcher.forwardMessageToUser(ctx, request);
    }

    @Override
    public void processOneway(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        if (request.getBodyCase() == ImMessage.BodyCase.HEARTBEAT_MESSAGE) {
            return;
        }
        gatewayClientDispatcher.forwardOnewayToUser(ctx, request);
    }
}
