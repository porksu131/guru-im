package com.guru.im.gateway.tcp.netty.server.processor;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.exception.ServiceException;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.core.manager.ChannelWrapper;
import com.guru.im.gateway.tcp.IMGatewayNettyClient;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.netty.server.ChannelAttributeUtils;
import com.guru.im.gateway.tcp.netty.server.dispatch.GatewayServerDispatcher;
import com.guru.im.gateway.tcp.service.AuthorizeService;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class AuthProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthProcessor.class);

    private final UserSessionManager userSessionManager;
    private final AuthorizeService authorizeService;
    private final IMGatewayNettyClient gatewayNettyClient;
    private final IMGatewayNettyServer gatewayNettyServer;
    private final ExecutorService serverMessageProcessorExecutor;
    private final GatewayServerDispatcher gatewayServerDispatcher;

    public AuthProcessor(UserSessionManager userSessionManager,
                         AuthorizeService authorizeService,
                         IMGatewayNettyClient gatewayNettyClient,
                         IMGatewayNettyServer gatewayNettyServer,
                         ExecutorService serverMessageProcessorExecutor,
                         GatewayServerDispatcher gatewayServerDispatcher) {
        this.userSessionManager = userSessionManager;
        this.authorizeService = authorizeService;
        this.gatewayNettyClient = gatewayNettyClient;
        this.gatewayNettyServer = gatewayNettyServer;
        this.serverMessageProcessorExecutor = serverMessageProcessorExecutor;
        this.gatewayServerDispatcher = gatewayServerDispatcher;
    }

    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage request) {
        if (request.getBodyCase() != ImMessage.BodyCase.AUTH_MESSAGE) {
            throw new ServiceException("未认证");
        }
        AuthMessage authMessage = request.getAuthMessage();
        if (authMessage.getAuthType() == AuthType.AUTH_LOGIN) {
            return login(ctx, request);
        } else if (authMessage.getAuthType() == AuthType.AUTH_LOGOUT) {
            return logout(ctx, request);
        }
        throw new ServiceException("未知的认证类型");
    }

    // 登录
    public ImMessage login(ChannelHandlerContext ctx, ImMessage request) {
        AuthMessage authMessage = request.getAuthMessage();
        DeviceInfo deviceInfo = authMessage.getDeviceInfo();
        //  验证用户信息
        Long userId = authorizeService.authenticate(authMessage.getToken());
        if (userId == null) {
            LOGGER.debug("消息[{}]认证失败！", request.getMsgId());
            return MessageBuilder.createImResponse(request, ResponseCode.AUTH_FAILED, "认证失败！");
        }

        ChannelAttributeUtils.setUserId(ctx.channel(), userId);
        ChannelAttributeUtils.setDeviceInfo(ctx.channel(), deviceInfo);

        String userAddr = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
        String gatewayAddr = gatewayNettyServer.getLocalServerAddress();

        // 将旧设备下线（如果存在的话）
        sendOfflineDevice(userId, deviceInfo.getDeviceId());

        // Local缓存用户会话信息
        gatewayNettyServer.getChannelCacheManager().saveUserDeviceChannelCache(
                userId, deviceInfo.getDeviceId(), new ChannelWrapper(userAddr, ctx.channel()));

        // Redis缓存会话信息，后面分发系统会用于消息转发，也就是根据用户id找到与用户连接的网关
        saveRedisSession(userId, deviceInfo, gatewayAddr, OnlineStatus.ONLINE);

        // 推送用户在线状态
        dispatchPresenceNotifyToDispatch(ctx, userId, UserPresence.ONLINE, PresenceReason.LOGIN, deviceInfo);

        LOGGER.debug("用户[{}]认证成功！", userId);
        return MessageBuilder.createImResponse(request, ResponseCode.SUCCESS, "认证成功！");
    }

    // 登出
    public ImMessage logout(ChannelHandlerContext ctx, ImMessage request) {
        AuthMessage authMessage = request.getAuthMessage();
        DeviceInfo deviceInfo = authMessage.getDeviceInfo();
        //  验证用户信息
        Long userId = authorizeService.authenticate(authMessage.getToken());
        if (userId == null) {
            LOGGER.debug("消息[{}]，验证token失败！", request.getMsgId());
            return MessageBuilder.createImResponse(request, ResponseCode.AUTH_FAILED, "登出失败！");
        }

        // 登出，移除Local中的用户会话信息
        gatewayNettyServer.getChannelCacheManager().removeUserDeviceChannelCache(userId, deviceInfo.getDeviceId());
        // 登出，更新redis中的用户会话信息
        saveRedisSession(userId, deviceInfo, null, OnlineStatus.OFFLINE);

        // 标识通道为登出
        ChannelAttributeUtils.setLogout(ctx.channel(), true);

        // 推送用户离线状态
        dispatchPresenceNotifyToDispatch(ctx, userId, UserPresence.OFFLINE, PresenceReason.LOGOUT, deviceInfo);
        LOGGER.debug("用户[{}]登出成功！", userId);
        return MessageBuilder.createImResponse(request, ResponseCode.SUCCESS, "登出成功！");
    }


    public void dispatchPresenceNotifyToDispatch(ChannelHandlerContext ctx,
                                                 Long userId,
                                                 UserPresence userPresence,
                                                 PresenceReason presenceReason,
                                                 DeviceInfo deviceInfo) {
        serverMessageProcessorExecutor.submit(() -> {
            PresenceNotify presenceNotify = PresenceNotify.newBuilder()
                    .setUserId(userId)
                    .setStatus(userPresence)
                    .setLastActive(System.currentTimeMillis())
                    .setReason(presenceReason)
                    .setDevice(deviceInfo)
                    .build();
            ImMessage onewayNotify = MessageBuilder.createPresenceNotify(presenceNotify);
            gatewayServerDispatcher.forwardOnewayToDispatch(ctx, onewayNotify);
        });
    }

    private void saveRedisSession(Long userId, DeviceInfo deviceInfo, String gatewayAddress, Integer onlineStatus) {
        userSessionManager.updateDeviceStatus(userId,
                deviceInfo.getDeviceId(),
                gatewayAddress,
                deviceInfo.getClientVersion(),
                deviceInfo.getPlatform().toString(),
                onlineStatus);
    }

    private String getGatewayClientAddr() {
        return this.gatewayNettyClient.getRemoteClientManager().getLocalAddress();
    }

    public void sendOfflineDevice(Long userId, String deviceId) {
        ChannelWrapper channelWrapper = gatewayNettyServer.getChannelCacheManager().getUserDeviceChannelCache(
                userId, deviceId);
        if (channelWrapper != null && channelWrapper.isOK()) {
            OfflineDeviceMessage offlineDeviceMessage = OfflineDeviceMessage.newBuilder()
                    .setUserId(userId)
                    .setDeviceId(deviceId)
                    .setOfflineType(OfflineDeviceType.SQUEEZE_OUT) // 被挤下线
                    .build();
            ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                    .setMsgType(ImMessage.MsgType.ONEWAY)
                    .setMessageType(MessageType.OFFLINE_DEVICE)
                    .setOfflineDeviceMessage(offlineDeviceMessage)
                    .build();
            this.gatewayNettyServer.getMessageProcessManager().writeMessage(channelWrapper.getChannel(), imMessage);
            ChannelAttributeUtils.setForceOffline(channelWrapper.getChannel(), true);
            ChannelUtil.closeChannel(channelWrapper.getChannel());
        }
    }
}
