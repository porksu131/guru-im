package com.guru.im.gateway.tcp.netty.server.listener;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.core.common.event.ChannelEventListener;
import com.guru.im.core.manager.ChannelCacheManager;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.netty.server.ChannelAttributeUtils;
import com.guru.im.gateway.tcp.netty.server.processor.AuthProcessor;
import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.protocol.model.PresenceReason;
import com.guru.im.protocol.model.UserPresence;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 家政服务监听器，当发生如下事件时，进行异步的额外处理
 */
@Component
public class HousekeepingChannelListener implements ChannelEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(HousekeepingChannelListener.class);
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    @Autowired
    private IMGatewayNettyServer gatewayNettyServer;
    @Autowired
    private ExecutorService serverMessageProcessorExecutor;

    @Autowired
    private UserSessionManager userSessionManager;

    @Autowired
    private AuthProcessor authProcessor;

    @Override
    public void onChannelConnect(String remoteAddr, ChannelHandlerContext ctx) {
        LOGGER.info("{} -- channel connected", remoteAddr);
    }

    @Override
    public void onChannelClose(String userAddr, ChannelHandlerContext ctx) {
        Boolean forceOffline = ChannelAttributeUtils.getForceOffline(ctx.channel());
        if (forceOffline != null && forceOffline) {
            return;
        }

        Boolean logout = ChannelAttributeUtils.getLogout(ctx.channel());
        if (logout != null && logout) {
            return;
        }
        // 上面两种是主动关闭连接的，无需处理，下面处理的是异常关闭场景
        Long uid = ChannelAttributeUtils.getUserId(ctx.channel());
        DeviceInfo deviceInfo = ChannelAttributeUtils.getDeviceInfo(ctx.channel());
        if (uid == null || deviceInfo == null) {
            return;
        }
        ChannelCacheManager channelCacheManager = gatewayNettyServer.getChannelCacheManager();

        // 移除Local的会话信息
        channelCacheManager.removeUserDeviceChannelCache(uid, deviceInfo.getDeviceId());
        channelCacheManager.removeRelatedDispatchChannel(uid, deviceInfo.getDeviceId());

        // 延迟5秒检测是否重连
        SCHEDULER.schedule(() -> {
            // 未重连则更新用户状态
            if (channelCacheManager.getUserDeviceChannelCache(uid, deviceInfo.getDeviceId()) == null) {
                // 更新redis用户的在线状态为离线
                userSessionManager.updateDeviceStatus(uid, deviceInfo.getDeviceId(), null,
                        deviceInfo.getClientVersion(), deviceInfo.getPlatform().toString(), OnlineStatus.OFFLINE);
                // 推送通知到分发系统
                pushOfflineNotifyToDispatch(ctx, uid, deviceInfo);
                LOGGER.info("感知到远程用户地址[{}]连接关闭，移除用户[{}]的会话信息", userAddr, uid);
            }
        }, 5, TimeUnit.SECONDS);

    }

    @Override
    public void onChannelException(String remoteAddr, ChannelHandlerContext ctx) {
        LOGGER.debug("{} -- channel exception", remoteAddr);
    }

    @Override
    public void onChannelIdle(String remoteAddr, ChannelHandlerContext ctx) {
        LOGGER.debug("{} -- channel idle", remoteAddr);
    }

    @Override
    public void onChannelActive(String remoteAddr, ChannelHandlerContext ctx) {
        // 在认证通过后才算连上，所以上线通知不在此处做
        LOGGER.debug("{} -- channel active", remoteAddr);
    }

    @Override
    public void onUnregistered(String remoteAddr, ChannelHandlerContext ctx) {
        LOGGER.debug("{} -- channel unregistered", remoteAddr);
    }

    private void pushOfflineNotifyToDispatch(ChannelHandlerContext ctx, Long uid, DeviceInfo deviceInfo) {
        serverMessageProcessorExecutor.submit(() -> {
            authProcessor.dispatchPresenceNotifyToDispatch(ctx, uid, UserPresence.OFFLINE, PresenceReason.TIMEOUT, deviceInfo);
        });
    }

}