package com.guru.im.disaptch.config;

import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.thread.ThreadFactoryImpl;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(DispatchNettyServerConfig.class)
public class IMNettyExecutorBean {
    @Autowired
    private DispatchNettyServerConfig nettyConfig;

    @Bean
    public EventExecutorGroup defaultEventExecutorGroup() {
        return new DefaultEventExecutorGroup(nettyConfig.getServerHandlerThreads(),
                new ThreadFactoryImpl("NettyServerHandlerThread_"));
    }

    @Bean
    public NettyEventExecutor nettyEventExecutor() {
        NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();
        // 此处可增加 nettyEventExecutor.registerListener(ChannelEventListener);
        return nettyEventExecutor;
    }

    @Bean
    public ExecutorService publicExecutor() {
        return Executors.newFixedThreadPool(4,
                new ThreadFactoryImpl("NettyServerPublicExecutor_"));
    }

    @Bean
    public ExecutorService bizProcessorExecutor() {
        return Executors.newFixedThreadPool(4,
                new ThreadFactoryImpl("NettyServerBizProcessorExecutor_"));
    }
}
