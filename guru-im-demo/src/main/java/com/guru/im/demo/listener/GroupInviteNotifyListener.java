package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.GroupInviteNotify;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class GroupInviteNotifyListener implements IMEventListener {
    private final MainFrame mainFrame;

    public GroupInviteNotifyListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.GROUP_INVITE_NOTIFY == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        GroupInviteNotify groupInviteNotify = event.getImMessage().getGroupInvite();
        this.mainFrame.onReceiveGroupInviteNotify(groupInviteNotify);
    }
}
