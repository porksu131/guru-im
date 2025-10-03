package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.PresenceNotify;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class PresenceNotifyListener implements IMEventListener {
    private final MainFrame mainFrame;

    public PresenceNotifyListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.USER_PRESENCE == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        PresenceNotify presenceNotify = event.getImMessage().getPresenceNotify();
        this.mainFrame.onReceivePresenceNotify(presenceNotify);
    }
}
