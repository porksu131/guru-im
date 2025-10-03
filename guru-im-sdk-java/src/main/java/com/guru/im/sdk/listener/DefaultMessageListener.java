package com.guru.im.sdk.listener;

import com.guru.im.core.im.listener.MessageListener;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.sdk.event.IMEventDispatcher;

public class DefaultMessageListener implements MessageListener {

    private final IMEventDispatcher eventDispatcher;

    public DefaultMessageListener(IMEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void onOnewayMessage(ImMessage request) {
        eventDispatcher.dispatchOneway(request);
    }

    @Override
    public ImMessage onMessage(ImMessage request) {
        return eventDispatcher.dispatch(request);
    }
}
