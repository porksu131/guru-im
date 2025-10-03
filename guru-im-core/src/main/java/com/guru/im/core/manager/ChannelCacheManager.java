package com.guru.im.core.manager;

import com.guru.im.core.common.util.ChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChannelCacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelCacheManager.class);
    //<netty服务端地址，netty客户端跟netty服务端的通道>，通用于netty客户端发送消息
    private final ConcurrentMap<String, ChannelWrapper> channelTables = new ConcurrentHashMap<>();
    //<用户id+deviceId，用户客户端跟网关连接的通道>，用于网关推送消息到用户
    private final ConcurrentMap<String, ChannelWrapper> userDeviceChannelTables = new ConcurrentHashMap<>();
    //<用户id+deviceId，分发系统地址>，用于网关路由一台与用户关联的分发系统服务端
    private final ConcurrentMap<String, String> relatedDispatchChannels = new ConcurrentHashMap<>();

    private final Lock connectedChannelsLock = new ReentrantLock();

    public ChannelCacheManager() {
    }

    public Channel getActiveChannel(final String addr) {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        return null;
    }

    public ChannelWrapper getChannelWrapper(final String addr) {
        return this.channelTables.get(addr);
    }

    public void closeChannel(String addr, Channel channel) {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw == null) {
            return;
        }
        this.closeChannel(cw.getChannel());
    }

    public void closeChannel(Channel channel) {
        int lockTimeOut = 3000;
        try {
            if (this.connectedChannelsLock.tryLock(lockTimeOut, TimeUnit.MILLISECONDS)) {
                try {
                    String addrRemote = ChannelUtil.parseChannelRemoteAddr(channel);
                    ChannelWrapper ChannelWrapper = this.channelTables.get(addrRemote);
                    if (ChannelWrapper != null && ChannelWrapper.isWrapperOf(channel)) {
                        this.channelTables.remove(addrRemote);
                    }
                    LOGGER.info("closeChannel: the channel[addr={}, id={}] was removed from channel table", addrRemote, channel.id());
                    ChannelUtil.closeChannel(channel);
                } catch (Exception e) {
                    LOGGER.error("closeChannel: close the channel[id={}] exception", channel.id(), e);
                } finally {
                    this.connectedChannelsLock.unlock();
                }
            } else {
                LOGGER.warn("closeChannel: try to lock channel table, but timeout, {}ms", lockTimeOut);
            }
        } catch (InterruptedException e) {
            LOGGER.error("closeChannel exception", e);
        }
    }

    public void updateChannelLastResponseTime(final String addr) {
        ChannelWrapper ChannelWrapper = this.channelTables.get(addr);
        if (ChannelWrapper != null && ChannelWrapper.isOK()) {
            ChannelWrapper.updateLastResponseTime();
        }
    }

    public void saveChannel(String remoteAddress, Channel channel) {
        this.channelTables.put(remoteAddress, new ChannelWrapper(remoteAddress, channel));
    }

    public void removeChannel(String remoteAddress, Channel channel) {
        ChannelWrapper ChannelWrapper = this.channelTables.get(remoteAddress);
        if (ChannelWrapper != null && ChannelWrapper.isWrapperOf(channel)) {
            this.channelTables.remove(remoteAddress);
        }
    }

    public void clear() {
        this.closeAllChannel();
        this.channelTables.clear();
        this.userDeviceChannelTables.clear();
        this.relatedDispatchChannels.clear();
    }

    public void closeAllChannel() {
        if (!this.channelTables.isEmpty()) {
            this.channelTables.values().forEach(wrapper ->
            {
                ChannelFuture closeFuture = wrapper.getChannel().close().awaitUninterruptibly();
                if (!closeFuture.isDone()) {
                    wrapper.getChannel().unsafe().closeForcibly();
                }
                if (closeFuture.isSuccess()) {
                    System.out.println("关闭成功");
                } else {
                    System.out.println("关闭失败: " + closeFuture.cause().getMessage());
                }
            });
        }
        if (!this.userDeviceChannelTables.isEmpty()) {
            this.channelTables.values().forEach(obj -> obj.getChannel().close().awaitUninterruptibly());
        }
    }

    public String getRelatedDispatchChannel(Long uid, String deviceId) {
        return this.relatedDispatchChannels.get(uid + "-" + deviceId);
    }

    public void saveRelatedDispatchChannel(Long uid, String deviceId, String addr) {
        this.relatedDispatchChannels.put(uid + "-" + deviceId, addr);
    }

    public void removeRelatedDispatchChannel(Long uid, String deviceId) {
        this.relatedDispatchChannels.remove(uid + "-" + deviceId);
    }

    public void removeUserDeviceChannelCache(Long uid, String deviceId) {
        this.userDeviceChannelTables.remove(uid + "-" + deviceId);
    }

    public ChannelWrapper getUserDeviceChannelCache(Long uid, String deviceId) {
        return this.userDeviceChannelTables.get(uid + "-" + deviceId);
    }

    public void saveUserDeviceChannelCache(Long uid, String deviceId, ChannelWrapper channelWrapper) {
        this.userDeviceChannelTables.put(uid + "-" + deviceId, channelWrapper);
    }
}
