package com.guru.im.gateway.tcp.config;

import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.thread.ThreadFactoryImpl;
import com.guru.im.gateway.tcp.IMGatewayNettyServer;
import com.guru.im.gateway.tcp.IMGatewayTcpServer;
import com.guru.im.gateway.tcp.IMGatewayWebSocketServer;
import com.guru.im.nacos.starter.NacosDiscovery;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(GatewayNettyServerConfig.class)
public class NettyServerConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public EventExecutorGroup defaultEventExecutorGroup(GatewayNettyServerConfig nettyConfig) {
        return new DefaultEventExecutorGroup(nettyConfig.getServerHandlerThreads(),
                new ThreadFactoryImpl("NettyServerHandlerThread_"));
    }

    @Bean
    public NettyEventExecutor nettyEventExecutor() {
        return new NettyEventExecutor();
    }

    @Bean
    public ExecutorService publicExecutor() {
        return Executors.newFixedThreadPool(4,
                new ThreadFactoryImpl("NettyServerPublicExecutor_"));
    }

    @Bean
    public ExecutorService sendCallBackExecutor() {
        return Executors.newFixedThreadPool(4,
                new ThreadFactoryImpl("NettyServerCallBackExecutor_"));
    }

    @Bean
    public ExecutorService serverMessageProcessorExecutor() {
        return Executors.newFixedThreadPool(4,
                new ThreadFactoryImpl("NettyServerMessageProcessorExecutor_"));
    }

    @Bean
    public IMGatewayNettyServer nettyServer(GatewayNettyServerConfig serverConfig,
                                            EventExecutorGroup defaultEventExecutorGroup,
                                            NettyEventExecutor nettyEventExecutor,
                                            ExecutorService sendCallBackExecutor,
                                            NacosDiscovery nacosDiscovery) {
        // 启动服务端
        if (applicationName.contains("websocket")) {
            return new IMGatewayWebSocketServer(serverConfig,
                    defaultEventExecutorGroup,
                    nettyEventExecutor,
                    sendCallBackExecutor,
                    nacosDiscovery);
        }
        return new IMGatewayTcpServer(serverConfig,
                defaultEventExecutorGroup,
                nettyEventExecutor,
                sendCallBackExecutor,
                nacosDiscovery);
    }
}
