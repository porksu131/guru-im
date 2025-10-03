package com.guru.im.disaptch.config;

import com.guru.im.core.server.config.NettyServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "im.dispatch.netty.server")
public class DispatchNettyServerConfig extends NettyServerConfig {

}
