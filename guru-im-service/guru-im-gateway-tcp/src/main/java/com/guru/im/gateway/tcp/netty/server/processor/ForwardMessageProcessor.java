package com.guru.im.gateway.tcp.netty.server.processor;

import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.netty.server.dispatch.GatewayServerDispatcher;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;

@Service("serverForwardMessageProcessor")
public class ForwardMessageProcessor implements MessageProcessor {

    private final IMGatewayNettyServer nettyServer;
    private final GatewayServerDispatcher gatewayServerDispatcher;

    public ForwardMessageProcessor(IMGatewayNettyServer nettyServer,
                                   GatewayServerDispatcher gatewayServerDispatcher) {
        this.nettyServer = nettyServer;
        this.gatewayServerDispatcher = gatewayServerDispatcher;
    }

    @Override
    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        if (request.getBodyCase() == ImMessage.BodyCase.AUTH_MESSAGE) {
            return nettyServer.getAuthDispatchProcessor().processRequest(ctx, request);
        }
        return gatewayServerDispatcher.forwardMessageToDispatch(ctx, request);
    }

    @Override
    public void processOneway(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        gatewayServerDispatcher.forwardOnewayToDispatch(ctx, request);
    }
}
