package com.guru.im.gateway.tcp;

import com.guru.im.core.coderc.ImMessageDecoder;
import com.guru.im.core.coderc.ImMessageEncoder;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.gateway.tcp.config.GatewayNettyServerConfig;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerAuthHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerConnectHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerBizHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerHeartbeatHandler;
import com.guru.im.nacos.starter.NacosDiscovery;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class IMGatewayTcpServer extends IMGatewayNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IMGatewayTcpServer.class);

    public IMGatewayTcpServer(GatewayNettyServerConfig serverConfig,
                              EventExecutorGroup defaultEventExecutorGroup,
                              NettyEventExecutor nettyEventExecutor,
                              ExecutorService sendCallBackExecutor,
                              NacosDiscovery nacosDiscovery) {
        super(serverConfig, defaultEventExecutorGroup, nettyEventExecutor, sendCallBackExecutor, nacosDiscovery);
    }

    @Override
    protected void initChannelHandler(SocketChannel channel) {
        NettyServerConnectHandler nettyServerConnectHandler = new NettyServerConnectHandler(nettyEventExecutor);
        NettyServerBizHandler nettyServerBizHandler = new NettyServerBizHandler(this);

        boolean channelExecutorEnable = serverConfig.isServerHandlerExecutorEnable();
        int idleTimeSeconds = serverConfig.getServerChannelMaxIdleTimeSeconds();

        channel.pipeline().addLast(channelExecutorEnable ? eventExecutorGroup : null
                , new ImMessageDecoder()  // 解码
                , new ImMessageEncoder()  // 编码
                , new IdleStateHandler(0, 0, idleTimeSeconds) // 空闲
                , new NettyServerAuthHandler(this) // 认证
                , new NettyServerHeartbeatHandler(this) // 心跳
                , nettyServerConnectHandler // 连接管理
                , nettyServerBizHandler // 业务处理
        );
    }

    public void start() throws Exception {
        super.start();
        LOGGER.info("tcpServer started !");
    }
}
