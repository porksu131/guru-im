package com.guru.im.gateway.tcp;


import com.guru.im.core.client.DefaultNettyClient;
import com.guru.im.core.client.handler.NettyClientHandler;
import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.core.starter.SpringBeanUtils;
import com.guru.im.gateway.tcp.config.GatewayNettyClientConfig;
import com.guru.im.gateway.tcp.config.GatewayNettyServerConfig;
import com.guru.im.gateway.tcp.netty.client.handler.NettyClientConnectHandler;
import com.guru.im.gateway.tcp.netty.client.manager.RemoteClientManager;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class IMGatewayNettyClient extends DefaultNettyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(IMGatewayNettyClient.class);
    private final RemoteClientManager remoteClientManager;
    private final String remoteNettyServiceName;
    private final int localServerPort;

    public IMGatewayNettyClient(
            GatewayNettyClientConfig nettyClientConfig,
            GatewayNettyServerConfig nettyServerConfig,
            RemoteClientManager remoteClientManager) {
        super(nettyClientConfig);
        this.nettyClientConfig = nettyClientConfig;
        this.remoteClientManager = remoteClientManager;
        this.remoteNettyServiceName = nettyClientConfig.getServerServiceName();
        this.localServerPort = nettyServerConfig.getListenPort();
    }

    public void start() {
        super.start();

        remoteClientManager.bindNettyClient(this);
        // 订阅服务变更
        remoteClientManager.subscribe(remoteNettyServiceName);

        // 初始连接所有服务
        remoteClientManager.connectToAllServers(remoteNettyServiceName);

        // 初始化消息处理器
        initMessageProcessor();
    }

    public void shutdown() {
        super.shutdown();
    }

    public void configChannelHandler(ChannelPipeline pipeline) {
        pipeline.addLast(defaultEventExecutorGroup, new NettyClientConnectHandler(nettyEventExecutor,this));
        pipeline.addLast(defaultEventExecutorGroup, new NettyClientHandler(this));
    }

    public RemoteClientManager getRemoteClientManager() {
        return remoteClientManager;
    }

    protected void initMessageProcessor() {
        ExecutorService executor = (ExecutorService) SpringBeanUtils.getBean("clientMessageProcessorExecutor");
        MessageProcessor messageProcessor = (MessageProcessor) SpringBeanUtils.getBean("clientForwardMessageProcessor");
        this.messageProcessManager.bindRequestProcessor(messageProcessor, executor);
    }

    public int getLocalServerPort() {
        return localServerPort;
    }

}
