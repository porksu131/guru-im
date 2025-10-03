package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.OfflineDeviceMessage;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class OfflineDeviceListener implements IMEventListener {
    private final MainFrame mainFrame;

    public OfflineDeviceListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.OFFLINE_DEVICE == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        OfflineDeviceMessage offlineDeviceMessage = event.getImMessage().getOfflineDeviceMessage();
        this.mainFrame.onReceiveOfflineDeviceMessage(offlineDeviceMessage);
    }
}
