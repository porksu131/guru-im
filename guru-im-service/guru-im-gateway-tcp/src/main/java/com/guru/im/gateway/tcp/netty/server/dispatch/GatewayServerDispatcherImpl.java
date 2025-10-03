package com.guru.im.gateway.tcp.netty.server.dispatch;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.manager.ChannelWrapper;
import com.guru.im.gateway.tcp.IMGatewayNettyClient;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.netty.server.ChannelAttributeUtils;
import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GatewayServerDispatcherImpl implements GatewayServerDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayServerDispatcherImpl.class);
    private static final long DEFAULT_TIME_OUT = 10000;
    @Autowired
    private IMGatewayNettyClient gatewayClient;
    @Autowired
    private IMGatewayNettyServer gatewayServer;

    @Override
    public ImMessage forwardMessageToDispatch(ChannelHandlerContext ctx, ImMessage request) {
        try {
            ChannelWrapper channelWrapper = checkAndGetDispatchChannel(ctx);
            if (channelWrapper == null) {
                return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "分发系统连接异常！");
            }
            String address = channelWrapper.getChannelAddress();
            Channel channel = channelWrapper.getChannel();
            // 发送消息到分发系统
            ImMessage imMessage = gatewayClient.sendSync(channel, request, DEFAULT_TIME_OUT);
            Response response = imMessage.getResponse();
            boolean success = response.getCode() == ResponseCode.SUCCESS;
            LOGGER.debug("网关转发消息到分发系统[{}]{}", address, success ? "成功" : "失败:" + response.getMsg());
            return MessageBuilder.createImResponse(request, response);

        } catch (SendRequestException | SendTimeoutException e) {
            LOGGER.error("网关转发消息到分发系统异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forwardOnewayToDispatch(ChannelHandlerContext ctx, ImMessage request) {
        try {
            ChannelWrapper channelWrapper = checkAndGetDispatchChannel(ctx);
            if (channelWrapper == null) {
                LOGGER.error("分发系统连接异常！");
                return;
            }
            String address = channelWrapper.getChannelAddress();
            Channel channel = channelWrapper.getChannel();
            // 发送单向消息到分发系统
            gatewayClient.getMessageProcessManager().invokeOneway(channel, request, DEFAULT_TIME_OUT);
            LOGGER.debug("网关已转发单向消息到分发系统[{}]",  address);
        } catch (SendRequestException | SendTimeoutException | InterruptedException e) {
            LOGGER.error("网关转发单向消息到分发系统异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private ChannelWrapper checkAndGetDispatchChannel(ChannelHandlerContext ctx) {
        // 优先选择已经连接过的分发系统
        Long userId = ChannelAttributeUtils.getUserId(ctx.channel());
        DeviceInfo deviceInfo = ChannelAttributeUtils.getDeviceInfo(ctx.channel());
        String remoteAddress = this.gatewayServer.getChannelCacheManager().getRelatedDispatchChannel(userId, deviceInfo.getDeviceId());
        if (remoteAddress != null) {
            Channel channel = gatewayClient.getRemoteClientManager().getDispatchConnection(remoteAddress);
            if (channel != null && channel.isActive()) {
                return new ChannelWrapper(remoteAddress, channel);
            }
        }

        // 首次连接，则轮询选择一个
        ChannelWrapper channelWrapper = gatewayClient.getRemoteClientManager().selectConnectionByUserId(userId);
        if (channelWrapper != null) {
            this.gatewayServer.getChannelCacheManager().saveRelatedDispatchChannel(userId, deviceInfo.getDeviceId(), channelWrapper.getChannelAddress());
        }
        return channelWrapper;
    }
}
