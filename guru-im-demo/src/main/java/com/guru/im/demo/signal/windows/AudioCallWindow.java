package com.guru.im.demo.signal.windows;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.signal.model.MediaDataClasses;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.protocol.model.MediaType;

/**
 * 语音通话窗口
 * 继承自VideoCallWindow
 */
public class AudioCallWindow extends VideoCallWindow {

    public AudioCallWindow(SignalingManager signalingManager, Long sessionId,
                           long targetUserId, String targetUserName, MainFrame parentWindow, boolean isIncomingCall) {
        super(signalingManager, sessionId, targetUserId, targetUserName,
                MediaType.MEDIA_AUDIO_ONLY, parentWindow, isIncomingCall);
    }

    @Override
    public void initializeBrowser(MediaDataClasses.CallInitData initData) {
        initializeBrowser("audio-call.html", initData); // 使用独立的音频通话页面
    }
}