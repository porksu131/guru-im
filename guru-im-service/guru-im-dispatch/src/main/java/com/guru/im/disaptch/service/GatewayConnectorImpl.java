package com.guru.im.disaptch.service;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.disaptch.IMDispatchNettyServer;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 网关连接器
 * 负责获取用户连接信息和向用户推送消息
 */
@Service
public class GatewayConnectorImpl implements GatewayConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayConnectorImpl.class);
    private static final long TIMEOUT_MILLIONS = 10000;
    private static final long BATCH_TIMEOUT_MILLIONS = 20000;

    @Autowired
    private UserSessionManager userSessionManager;

    @Autowired
    private IMDispatchNettyServer nettyServer;


    @Override
    public List<DeviceStatus> getUserAllGatewayNodes(Long uid) {
        return userSessionManager.getAllDeviceRouteInfo(uid);
    }

    @Override
    public List<DeviceStatus> getUserDeviceStatus(Long uid, int onlineStatus) {
        return userSessionManager.getUserDeviceStatus(uid, onlineStatus);
    }

    @Override
    public DeviceStatus getUserDeviceGatewayNode(Long uid, String deviceId) {
        return userSessionManager.getDeviceRoute(uid, deviceId);
    }

    @Override
    public CompletableFuture<ImMessage> pushMessageToGateway(Long uid, DeviceStatus gatewayRoute, ImMessage imMessage) {
        CompletableFuture<ImMessage> future = new CompletableFuture<>();
        Channel channel = findUserGatewayChannel(gatewayRoute);
        if (channel == null) {
            future.complete(MessageBuilder.createImResponse(imMessage, ResponseCode.SYSTEM_ERROR, "目标用户设备离线"));
            return future;
        }
        // 封装额外字段，用于网关找到用户设备的连接
        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("toUserId", String.valueOf(uid));
        extraFields.put("toDeviceId", gatewayRoute.getDeviceId());

        ImMessage request = ImMessage.newBuilder(imMessage).putAllExtraFields(extraFields).build();
        dispatchMessageToUser(channel, request, TIMEOUT_MILLIONS, future);
        LOGGER.info("pushMessageToGateway: uid={}, deviceId={}, gateway={}",
                uid, gatewayRoute.getDeviceId(), gatewayRoute.getGatewayAddress());
        return future;
    }

    @Override
    public CompletableFuture<List<ImMessage>> pushMessageToGateways(Long uid,
                                                                    List<DeviceStatus> gatewayRoutes,
                                                                    ImMessage imMessage) {
        CompletableFuture<List<ImMessage>> resFuture = new CompletableFuture<>();
        List<ImMessage> responseList = new ArrayList<>();
        for (DeviceStatus gatewayRoute : gatewayRoutes) {
            CompletableFuture<ImMessage> future = pushMessageToGateway(uid, gatewayRoute, imMessage);
            responseList.add(future.join());
        }
        resFuture.complete(responseList);
        return resFuture;
    }

    private Channel findUserGatewayChannel(DeviceStatus deviceStatus) {
        if (deviceStatus != null && deviceStatus.getGatewayAddress() != null) {
            Channel channel = nettyServer.getChannelCacheManager().getActiveChannel(deviceStatus.getGatewayAddress());
            LOGGER.info("findUserGatewayChannel: gatewayAddress={}, resultChannel isNull={}",
                    deviceStatus.getGatewayAddress(), channel == null);
            if (channel != null && channel.isActive()) {
                return channel;
            }
        }
        return null;
    }

    private void dispatchMessageToUser(Channel channel, ImMessage request, long timeout, CompletableFuture<ImMessage> future) {
        nettyServer.getMessageProcessManager().sendAsync(channel, request, timeout, new InvokeCallback() {
            @Override
            public void operationSucceed(ImMessage imResponse) {
                future.complete(imResponse);
            }

            @Override
            public void operationFail(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
    }

    @Override
    public void pushMessageByOneway(Long uid, List<DeviceStatus> gatewayRoutes, ImMessage imMessage) {
        for (DeviceStatus gatewayRoute : gatewayRoutes) {
            pushMessageByOneway(uid, gatewayRoute, imMessage);
        }
    }

    @Override
    public void pushMessageByOneway(Long uid, DeviceStatus deviceStatus, ImMessage imMessage) {
        Channel channel = findUserGatewayChannel(deviceStatus);
        if (channel == null) {
            LOGGER.error("pushMessageToGateway oneway failed, channel is null, uid={}", uid);
            return;
        }
        // 封装额外字段，用于网关找到用户设备的连接
        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("toUserId", String.valueOf(uid));
        extraFields.put("toDeviceId", deviceStatus.getDeviceId());

        ImMessage request = ImMessage.newBuilder(imMessage).putAllExtraFields(extraFields).build();
        try {
            nettyServer.getMessageProcessManager().invokeOneway(channel, request, TIMEOUT_MILLIONS);
            LOGGER.info("pushMessageToGateway oneway: uid={}, deviceId={}, gateway={}",
                    uid, deviceStatus.getDeviceId(), deviceStatus.getGatewayAddress());
        } catch (Exception e) {
            LOGGER.error("pushMessageToGateway oneway failed, {}, uid={}", e.getMessage(), uid);
        }
    }
}