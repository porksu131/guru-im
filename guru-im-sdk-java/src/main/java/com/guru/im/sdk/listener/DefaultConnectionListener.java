package com.guru.im.sdk.listener;

import com.guru.im.core.im.listener.ConnectionListener;
import com.guru.im.sdk.constant.ConnectionConstant;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventDispatcher;
import com.guru.im.sdk.event.IMEventType;

public class DefaultConnectionListener implements ConnectionListener {
    private final IMEventDispatcher eventDispatcher;

    public DefaultConnectionListener(IMEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void onConnected() {
        eventDispatcher.dispatchOneway(new IMEvent(IMEventType.CONNECTION_CHANGED, null, ConnectionConstant.CONNECTED));
    }

    @Override
    public void onDisconnected() {
        eventDispatcher.dispatchOneway(new IMEvent(IMEventType.CONNECTION_CHANGED, null, ConnectionConstant.DISCONNECTED));
    }

    @Override
    public void onConnectFailed(String error) {
        eventDispatcher.dispatchOneway(new IMEvent(IMEventType.CONNECTION_CHANGED, null, ConnectionConstant.CONNECT_FAILED));
    }

    @Override
    public void onReconnecting(int attemptCount) {
        eventDispatcher.dispatchOneway(new IMEvent(IMEventType.CONNECTION_CHANGED, null, ConnectionConstant.CONNECTING));
    }
}
