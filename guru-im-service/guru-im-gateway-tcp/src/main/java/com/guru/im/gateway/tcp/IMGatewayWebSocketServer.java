package com.guru.im.gateway.tcp;

import com.guru.im.core.coderc.ImMessageDecoder;
import com.guru.im.core.coderc.ImMessageEncoder;
import com.guru.im.core.coderc.ImWebSocketIDecoder;
import com.guru.im.core.coderc.ImWebSocketIEncoder;
import com.guru.im.core.common.event.NettyEventExecutor;
import com.guru.im.gateway.tcp.config.GatewayNettyServerConfig;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerAuthHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerBizHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerConnectHandler;
import com.guru.im.gateway.tcp.netty.server.handler.NettyServerHeartbeatHandler;
import com.guru.im.nacos.starter.NacosDiscovery;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class IMGatewayWebSocketServer extends IMGatewayNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IMGatewayWebSocketServer.class);

    public IMGatewayWebSocketServer(GatewayNettyServerConfig serverConfig,
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
        NettyServerAuthHandler nettyServerAuthHandler = new NettyServerAuthHandler(this);
        NettyServerHeartbeatHandler nettyServerHeartbeatHandler = new NettyServerHeartbeatHandler(this);


        // 添加协议转换层，可以沿用以前的tcp逻辑
        ChannelPipeline pipeline = channel.pipeline();

        // ================ WebSocket协议层 ================
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerProtocolHandler(
                "/im",
                null,
                true,
                65536,
                false,
                true));

        // ================ 协议转换层 =====================
        pipeline.addLast(new ImWebSocketIDecoder());
        pipeline.addLast(new ImWebSocketIEncoder());

        // ================ 原有TCP协议层 ===================
        pipeline.addLast(new ImMessageDecoder()); // ByteBuf → 业务对象
        pipeline.addLast(new ImMessageEncoder()); // 业务对象 → ByteBuf
        pipeline.addLast(nettyServerAuthHandler); // 认证
        pipeline.addLast(nettyServerHeartbeatHandler); // 心跳
        pipeline.addLast(nettyServerConnectHandler); // 连接管理
        pipeline.addLast(nettyServerBizHandler); // 业务处理

    }

    public void start() throws Exception {
        super.start();
        LOGGER.info("webSocketServer started !");
    }
}
