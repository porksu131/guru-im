package com.guru.im.gateway.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;


@Component
public class ApplicationCloseListener implements ApplicationListener<ContextClosedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ApplicationCloseListener.class);
    private final IMGatewayNettyClient nettyClient;
    private final IMGatewayNettyServer nettyServer;

    public ApplicationCloseListener(
            IMGatewayNettyClient nettyClient,
            IMGatewayNettyServer nettyServer) {
        this.nettyClient = nettyClient;
        this.nettyServer = nettyServer;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("ApplicationCloseEvent");
        nettyClient.shutdown();
        nettyServer.shutdown();
        log.info("nettyClient and nettyServer had shutdown");
    }

}
