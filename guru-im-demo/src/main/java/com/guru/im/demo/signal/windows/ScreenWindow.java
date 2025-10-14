package com.guru.im.demo.signal.windows;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.demo.signal.model.MediaDataClasses;
import com.guru.im.protocol.model.MediaType;

public class ScreenWindow extends VideoCallWindow{
    public ScreenWindow(SignalingManager signalingManager, Long sessionId, long targetUserId, String targetUserName,
                        MainFrame parentWindow, boolean isIncomingCall) {
        super(signalingManager, sessionId, targetUserId, targetUserName,
                MediaType.MEDIA_SCREEN_SHARE, parentWindow, isIncomingCall);
    }

    @Override
    public void initializeBrowser(MediaDataClasses.CallInitData initData) {
        initializeBrowser("screen-share.html", initData); // 使用独立的屏幕共享页面
    }
}
