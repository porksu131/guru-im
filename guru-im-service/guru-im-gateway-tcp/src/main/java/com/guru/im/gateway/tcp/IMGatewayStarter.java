package com.guru.im.gateway.tcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class IMGatewayStarter implements CommandLineRunner {
    @Autowired
    private IMGatewayNettyServer nettyServer;

    @Autowired
    private IMGatewayNettyClient nettyClient;

    @Override
    public void run(String... args) throws Exception {
        // 启动客户端
        nettyClient.start();

        // 启动服务端
        nettyServer.start();
    }
}
