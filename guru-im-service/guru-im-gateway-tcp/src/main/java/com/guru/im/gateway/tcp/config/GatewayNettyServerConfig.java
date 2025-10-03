package com.guru.im.gateway.tcp.config;

import com.guru.im.core.server.config.NettyServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "im.gateway.netty.server")
public class GatewayNettyServerConfig extends NettyServerConfig {

}
