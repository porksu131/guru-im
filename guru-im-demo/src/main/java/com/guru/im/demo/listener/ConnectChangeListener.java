package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class ConnectChangeListener implements IMEventListener {

    private final MainFrame mainFrame;

    public ConnectChangeListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.CONNECTION_CHANGED == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        String connectEvent = (String) event.getData();
        mainFrame.onReceiveConnectChange(connectEvent);
    }
}
