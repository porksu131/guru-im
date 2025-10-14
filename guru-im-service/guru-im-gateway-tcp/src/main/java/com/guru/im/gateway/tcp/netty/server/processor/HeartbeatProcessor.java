package com.guru.im.gateway.tcp.netty.server.processor;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.gateway.tcp.IMGatewayNettyClient;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.netty.server.ChannelAttributeUtils;
import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class HeartbeatProcessor{
    private static Logger LOGGER = LoggerFactory.getLogger(HeartbeatProcessor.class);
    @Autowired
    private ExecutorService serverMessageProcessorExecutor;
    @Autowired
    private UserSessionManager userSessionManager;
    @Autowired
    private IMGatewayNettyServer nettyServer;

    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) {
        serverMessageProcessorExecutor.submit(() -> {
            try {
                Long userId = ChannelAttributeUtils.getUserId(ctx.channel());
                DeviceInfo deviceInfo = ChannelAttributeUtils.getDeviceInfo(ctx.channel());

                // 更新用户活跃时间
                userSessionManager.updateDeviceStatus(userId,
                        deviceInfo.getDeviceId(),
                        nettyServer.getLocalServerAddress(),
                        deviceInfo.getClientVersion(),
                        deviceInfo.getPlatform().toString(),
                        OnlineStatus.ONLINE);
            } catch (Exception e) {
                LOGGER.error("心跳处理异常：{}", e.getMessage(), e);
            }

        });
        // 返回一个心跳信息
        return MessageBuilder.createHeartbeat();
    }
}
