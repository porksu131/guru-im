package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.FriendRequestNotify;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class FriendRequestNotifyListener implements IMEventListener {
    private final MainFrame mainFrame;

    public FriendRequestNotifyListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.FRIEND_REQUEST_NOTIFY == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        FriendRequestNotify friendRequestNotify = event.getImMessage().getFriendRequest();
        this.mainFrame.onReceiveFriendRequestNotify(friendRequestNotify);
    }
}
