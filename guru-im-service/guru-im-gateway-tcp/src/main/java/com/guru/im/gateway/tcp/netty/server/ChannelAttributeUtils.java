package com.guru.im.gateway.tcp.netty.server;

import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.protocol.model.GatewayInfo;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class ChannelAttributeUtils {
    private static final AttributeKey<Long> USER_KEY = AttributeKey.valueOf("userId");
    private static final AttributeKey<DeviceInfo> DEVICE_KEY = AttributeKey.valueOf("deviceInfo");
    private static final AttributeKey<GatewayInfo> GATEWAY_KEY = AttributeKey.valueOf("gatewayInfo");
    private static final AttributeKey<Boolean> FORCE_OFFLINE_KEY = AttributeKey.valueOf("forceOffline");
    private static final AttributeKey<Boolean> LOGOUT_KEY = AttributeKey.valueOf("logout");

    public static void setUserId(Channel channel, Long userId) {
        channel.attr(USER_KEY).set(userId);
    }

    public static Long getUserId(Channel channel) {
        return channel.attr(USER_KEY).get();
    }

    public static void setDeviceInfo(Channel channel, DeviceInfo deviceInfo) {
        channel.attr(DEVICE_KEY).set(deviceInfo);
    }

    public static DeviceInfo getDeviceInfo(Channel channel) {
        return channel.attr(DEVICE_KEY).get();
    }

    public static void setGatewayInfo(Channel channel, GatewayInfo gatewayInfo) {
        channel.attr(GATEWAY_KEY).set(gatewayInfo);
    }

    public static GatewayInfo getGatewayInfo(Channel channel) {
        return channel.attr(GATEWAY_KEY).get();
    }

    public static void setForceOffline(Channel channel, Boolean forceOffline) {
        channel.attr(FORCE_OFFLINE_KEY).set(forceOffline);
    }
    public static Boolean getForceOffline(Channel channel) {
        return channel.attr(FORCE_OFFLINE_KEY).get();
    }

    public static void setLogout(Channel channel, Boolean logout) {
        channel.attr(LOGOUT_KEY).set(logout);
    }

    public static Boolean getLogout(Channel channel) {
        return channel.attr(LOGOUT_KEY).get();
    }
}