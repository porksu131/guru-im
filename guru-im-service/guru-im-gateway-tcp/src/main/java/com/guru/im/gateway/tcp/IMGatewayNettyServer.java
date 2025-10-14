package com.guru.im.gateway.tcp;

import com.guru.im.common.utils.NetworkUtils;
import com.guru.im.core.common.event.ChannelEventListener;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.core.common.thread.ThreadFactoryImpl;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.core.server.BaseNettyServer;
import com.guru.im.core.server.config.NettyServerConfig;
import com.guru.im.core.starter.SpringBeanUtils;
import com.guru.im.gateway.tcp.netty.server.processor.AuthProcessor;
import com.guru.im.gateway.tcp.netty.server.processor.HeartbeatProcessor;
import com.guru.im.nacos.starter.NacosDiscovery;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class IMGatewayNettyServer extends BaseNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IMGatewayNettyServer.class);

    private final EventLoopGroup eventLoopGroupBoss;
    private final EventLoopGroup eventLoopGroupSelector;

    protected final NettyServerConfig serverConfig; // 服务端配置
    protected final NettyEventExecutor nettyEventExecutor; // channel事件守护执行器
    protected final EventExecutorGroup eventExecutorGroup; // channelHandler的线程池
    protected final NacosDiscovery nacosDiscovery;
    protected AuthProcessor authProcessor;
    protected HeartbeatProcessor heartbeatProcessor;
    private int localServerPort;

    public IMGatewayNettyServer(NettyServerConfig serverConfig,
                                EventExecutorGroup eventExecutorGroup,
                                NettyEventExecutor nettyEventExecutor,
                                ExecutorService sendCallBackExecutor,
                                NacosDiscovery nacosDiscovery) {
        this.messageProcessManager.setCallbackExecutor(sendCallBackExecutor);
        this.serverConfig = serverConfig;
        this.eventExecutorGroup = eventExecutorGroup;
        this.nettyEventExecutor = nettyEventExecutor;
        this.eventLoopGroupBoss = buildEventLoopGroupBoss();
        this.eventLoopGroupSelector = buildEventLoopGroupSelector();
        this.nacosDiscovery = nacosDiscovery;
        this.localServerPort = serverConfig.getListenPort();
    }

    protected abstract void initChannelHandler(SocketChannel channel);

    public void start() throws Exception {
        // 启动netty服务
        nettyServerStart();
        nettyEventExecutor.start();

        // 服务注册 nacos
        String hostName = InetAddress.getLocalHost().getHostName();
        nacosDiscovery.registerInstance(serverConfig.getServiceName(), hostName, serverConfig.getListenPort());

        // 初始化消息处理器
        initMessageProcessor();

        // 初始化netty事件监听器
        initChannelEventListener();
    }


    public void nettyServerStart() {
        try {
            ServerBootstrap serverBootstrap = initServerBootstrap();
            ChannelFuture sync = serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            LOGGER.info("netty server started, listening {}:{}",
                    addr.getAddress().getHostAddress(), serverConfig.getListenPort());
        } catch (Exception e) {
            String format = String.format("Failed to bind to %s:%d",
                    serverConfig.getBindAddress(), serverConfig.getListenPort());
            throw new IllegalStateException(format, e);
        }
    }


    public void shutdown() {
        try {
            Thread.sleep(Duration.ofSeconds(serverConfig.getShutdownWaitTimeSeconds()).toMillis());

            this.getMessageProcessManager().clear();

            if (this.eventLoopGroupBoss != null) {
                this.eventLoopGroupBoss.shutdownGracefully();
            }

            if (this.eventLoopGroupSelector != null) {
                this.eventLoopGroupSelector.shutdownGracefully();
            }

            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }

            if (this.eventExecutorGroup != null) {
                this.eventExecutorGroup.shutdownGracefully();
            }

        } catch (Exception e) {
            LOGGER.error("{} shutdown exception, ", serverConfig.getServiceName(), e);
        }
    }

    private EventLoopGroup buildEventLoopGroupSelector() {
        if (useEpoll()) {
            return new EpollEventLoopGroup(serverConfig.getServerWorkerThreads(),
                    new ThreadFactoryImpl("NettyServerEPOLLSelector_"));
        } else {
            return new NioEventLoopGroup(serverConfig.getServerWorkerThreads(),
                    new ThreadFactoryImpl("NettyServerNIOSelector_"));
        }
    }

    private EventLoopGroup buildEventLoopGroupBoss() {
        if (useEpoll()) {
            return new EpollEventLoopGroup(1,
                    new ThreadFactoryImpl("NettyEPOLLBoss_"));
        } else {
            return new NioEventLoopGroup(1,
                    new ThreadFactoryImpl("NettyNIOBoss_"));
        }
    }

    private boolean useEpoll() {
        return NetworkUtils.isLinuxPlatform() && Epoll.isAvailable();
    }

    protected ServerBootstrap initServerBootstrap() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, serverConfig.getServerSocketBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(new InetSocketAddress(serverConfig.getBindAddress(), serverConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) {
                        initChannelHandler(channel);
                    }
                });

        if (serverConfig.isServerPooledByteBufAllocatorEnable()) {
            serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        return serverBootstrap;
    }

    protected void initMessageProcessor() {
        ExecutorService executor = (ExecutorService) SpringBeanUtils.getBean("serverMessageProcessorExecutor");
        MessageProcessor messageProcessor = (MessageProcessor) SpringBeanUtils.getBean("serverForwardMessageProcessor");
        this.messageProcessManager.bindRequestProcessor(messageProcessor, executor);
        this.authProcessor = (AuthProcessor) SpringBeanUtils.getBean("authProcessor");
        this.heartbeatProcessor = (HeartbeatProcessor) SpringBeanUtils.getBean("heartbeatProcessor");
    }

    protected void initChannelEventListener() {
        List<ChannelEventListener> channelEventListeners = SpringBeanUtils.getBeansOfType(ChannelEventListener.class);
        channelEventListeners.forEach(this.nettyEventExecutor::registerListener);
    }

    public String getLocalServerAddress() {
        return ChannelUtil.getLocalHost() + ":" + getLocalServerPort();
    }

    public int getLocalServerPort() {
        return localServerPort;
    }

    public AuthProcessor getAuthDispatchProcessor() {
        return authProcessor;
    }

    public HeartbeatProcessor getHeartbeatProcessor() {
        return heartbeatProcessor;
    }
}
