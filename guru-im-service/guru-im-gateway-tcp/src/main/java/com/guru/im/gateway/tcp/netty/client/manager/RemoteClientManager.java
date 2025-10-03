package com.guru.im.gateway.tcp.netty.client.manager;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.guru.im.core.client.BaseNettyClient;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.core.manager.ChannelWrapper;
import com.guru.im.nacos.starter.NacosDiscovery;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Component
public class RemoteClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClientManager.class);
    private final ConnectionManager connectionManager;
    private final ExecutorService connectionExecutor;
    private final NacosDiscovery nacosDiscovery;
    private BaseNettyClient nettyClient;
    private String localAddress;

    public RemoteClientManager(ConnectionManager connectionManager,
                               ExecutorService connectionExecutor,
                               NacosDiscovery nacosDiscovery) {
        this.connectionManager = connectionManager;
        this.connectionExecutor = connectionExecutor;
        this.nacosDiscovery = nacosDiscovery;
    }


    public void subscribe(String serviceName) {
        nacosDiscovery.subscribe(serviceName, this::handleServerInstancesChange);
    }

    private void handleServerInstancesChange(List<Instance> newInstances) {
        Set<String> newAddresses = new HashSet<>();

        // 处理新增实例
        for (Instance instance : newInstances) {
            String address = connectionManager.getAddress(instance);
            newAddresses.add(address);

            if (!connectionManager.hasConnection(address)) {
                connectToServer(instance);
            }
        }

        // 处理下线实例
        connectionManager.getAllConnections().keySet().forEach(address -> {
            if (!newAddresses.contains(address)) {
                connectionManager.removeConnection(address);
                LOGGER.info("下线服务实例：{}", address);
            }
        });
    }

    public void connectToAllServers(String serviceName) {
        List<Instance> instances = nacosDiscovery.getInstances(serviceName);
        if (instances != null) {
            instances.forEach(this::connectToServer);
        }
    }

    public void connectToServer(Instance instance) {
        connectionExecutor.submit(() -> {
            try {
                String address = connectionManager.getAddress(instance);
                if (connectionManager.isActive(address)) {
                    return;
                }
                Channel channel = nettyClient.connect(address);
                if (localAddress == null || localAddress.isEmpty()) {
                    localAddress = ChannelUtil.parseChannelLocalAddr(channel);
                }
                connectionManager.addConnection(instance, channel);
            } catch (Exception e) {
                handleConnectFailure(instance, e);
            }
        });
    }

    public void handleConnectFailure(Instance instance, Throwable cause) {
        String address = connectionManager.getAddress(instance);
        System.err.println("连接失败: " + address + ", 原因: " + cause.getMessage());
        // 重连
        connectionManager.scheduleReconnect(instance, () -> {
            connectToServer(instance);
        });
    }

    // 路由到指定用户的分发层
    public ChannelWrapper selectConnectionByUserId(long userId) {
        String remoteAddress = connectionManager.getUserConnection(userId);
        if (remoteAddress != null) {
            Channel channel = connectionManager.getConnectionChannel(remoteAddress);
            if (channel != null && channel.isActive()) {
                return new ChannelWrapper(remoteAddress, channel);
            }
        }
        return getChannelLoadBalance();
    }

    public Channel getDispatchConnection(String address) {
        return connectionManager.getConnectionChannel(address);
    }

    // 轮询选择一个分发系统服务端。添加channel的检查和重试，避免因为选择到了一个恰好下线但本地未来得及移除的channel。
    public ChannelWrapper getChannelLoadBalance() {
        int maxTry = 3;
        int tryCount = 0;
        do {
            String address = connectionManager.selectOneByRoundRobin();
            Channel channel = connectionManager.getConnectionChannel(address);
            if (channel != null && channel.isActive()) {
                return new ChannelWrapper(address, channel);
            }
            tryCount++;
        } while (tryCount < maxTry);
        return null;
    }

    public void bindNettyClient(BaseNettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    public String getLocalAddress() {
        return localAddress;
    }
}
