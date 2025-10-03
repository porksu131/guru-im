package com.guru.im.gateway.tcp.netty.client.dispatch;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.manager.ChannelWrapper;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.protocol.util.MessageHelper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GatewayClientDispatcherImpl implements GatewayClientDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayClientDispatcherImpl.class);
    private static final long DEFAULT_TIME_OUT = 10000;
    @Autowired
    private IMGatewayNettyServer nettyServer;

    @Override
    public ImMessage forwardMessageToUser(ChannelHandlerContext ctx, ImMessage request) {
        try {
            // 解析消息 获取要发送的目标用户id
            Long toUserId = MessageHelper.getToUserId(request);
            String toDeviceId = MessageHelper.getToDeviceId(request);

            // 找到网关的nett服务端与用户连接的通道
            ChannelWrapper userChannel = nettyServer.getChannelCacheManager().getUserDeviceChannelCache(toUserId, toDeviceId);
            if (userChannel != null && userChannel.isOK()) {
                // 网关的nett服务端进行转发消息到用户
                ImMessage imMessage = nettyServer.getMessageProcessManager().sendSync(
                        userChannel.getChannel(),
                        request,
                        DEFAULT_TIME_OUT);
                Response response = imMessage.getResponse();
                boolean success = ResponseCode.SUCCESS == response.getCode();
                LOGGER.debug("消息转发到用户[{}][{}]：{}", toUserId, toDeviceId, success ? "成功" : "失败:" + response.getMsg());
                return MessageBuilder.createImResponse(request, response);
            }
            return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "用户[" + toUserId + "][" + toDeviceId + "]不在线！");
        } catch (IllegalArgumentException e) {
            LOGGER.error("消息转换异常：{}", e.getMessage());
            return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "消息未成功转发：" + e.getMessage());
        } catch (SendRequestException | SendTimeoutException e) {
            LOGGER.error("消息转发超时：{}", e.getMessage());
            return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "消息转发超时：" + e.getMessage());
        }
    }

    @Override
    public void forwardOnewayToUser(ChannelHandlerContext ctx, ImMessage request) {
        try {
            // 解析消息 获取要发送的目标用户id
            Long toUserId = MessageHelper.getToUserId(request);
            String toDeviceId = MessageHelper.getToDeviceId(request);
            // 找到网关的nett服务端与用户连接的通道
            ChannelWrapper userChannel = nettyServer.getChannelCacheManager().getUserDeviceChannelCache(toUserId, toDeviceId);
            if (userChannel == null || !userChannel.isOK()) {
                LOGGER.error("用户[{}][{}]连接异常，推送通知失败！", toUserId, toDeviceId);
                return;
            }
            // 发送单向消息到用户
            nettyServer.sendOneway(userChannel.getChannel(), request, DEFAULT_TIME_OUT);
            LOGGER.debug("网关已转发单向消息到用户[{}][{}]", toUserId, toDeviceId);
        } catch (IllegalArgumentException e) {
            LOGGER.error("网关消息转换异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (SendRequestException | SendTimeoutException | InterruptedException e) {
            LOGGER.error("网关转发单向消息到用户异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
