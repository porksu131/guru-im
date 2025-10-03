package com.guru.im.disaptch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationCloseListener implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    private IMDispatchNettyServer nettyServer;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        nettyServer.shutdown();
    }

}
