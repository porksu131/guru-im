package com.guru.im.demo.signal.windows;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.demo.signal.model.MediaDataClasses;
import com.guru.im.demo.util.ImageUtil;
import com.guru.im.protocol.model.CallHangup;
import com.guru.im.protocol.model.MediaType;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;

import javax.swing.*;

/**
 * 音视频通话窗口
 */
public class VideoCallWindow extends AbstractMediaWindow {
    private final long targetUserId;
    private final String targetUserName;
    private final boolean isIncomingCall;
    private MediaDataClasses.CallInitData callInitData;

    public VideoCallWindow(SignalingManager signalingManager, Long sessionId,
                           long targetUserId, String targetUserName,
                           MediaType mediaType, MainFrame parentWindow, boolean isIncomingCall) {
        super(signalingManager, mediaType, parentWindow, sessionId);

        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.isIncomingCall = isIncomingCall;

        // 创建初始化数据
        this.callInitData = new MediaDataClasses.CallInitData(
                sessionId, targetUserId, targetUserName,
                isIncomingCall, currentUser, deviceInfo
        );

        initializeUI();
        initializeBrowser(callInitData);
    }

    private void initializeUI() {
        String callType = getMediaTypeName();
        String title = isIncomingCall ?
                String.format("来电 - %s的%s", targetUserName, callType) :
                String.format("%s - 与%s", callType, targetUserName);

        setTitle(title);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (mediaType == MediaType.MEDIA_SCREEN_SHARE) {
            setSize(900, 750);
        } else {
            setSize(400, 600);
        }

        setLocationRelativeTo(parentWindow);
        setResizable(true);
        setAlwaysOnTop(false);

        // 设置窗口图标
        ImageIcon icon = createWindowIcon();
        if (icon != null) {
            setIconImage(icon.getImage());
        }

        addCommonWindowListener(this::handleWindowClosing);
    }

    private ImageIcon createWindowIcon() {
        if (MediaType.MEDIA_VIDEO == this.mediaType) {
            return ImageUtil.createSVGIcon("image/video.svg", 16);
        } else if (MediaType.MEDIA_AUDIO_ONLY == this.mediaType) {
            return ImageUtil.createSVGIcon("image/phone.svg", 16);
        } else if (MediaType.MEDIA_SCREEN_SHARE == this.mediaType) {
            return ImageUtil.createSVGIcon("image/screen.svg", 16);
        }
        return null;
    }

    public void initializeBrowser(MediaDataClasses.CallInitData initData) {
        super.initializeBrowser("video-call.html", initData); // 使用独立的视频通话页面
    }


    /**
     * 通话被接受
     */
    public void onCallAccepted(boolean isSelfCall) {
        updateWindowTitle("通话中");
        if (!isSelfCall) {
            if (this.bridgeHandler != null) {
                this.bridgeHandler.sendCallAccepted();
            }
        }
    }

    /**
     * 通话被拒绝
     */
    public void onCallRejected(boolean isSelfCall) {
        if (this.bridgeHandler != null) {
            this.bridgeHandler.sendCallRejected(isSelfCall ? "我拒绝了通话" : "对方拒绝了通话");
        }
    }

    /**
     * 通话挂断
     */
    public void onCallHangup(boolean isSelfCall) {
        if (!isSelfCall) {
            if (this.bridgeHandler != null) {
                this.bridgeHandler.sendCallHangup();
            }
        }
    }

    /**
     * 通话超时
     */
    public void onCallTimeout(boolean isSelfCall) {
        if (!isSelfCall) {
            if (bridgeHandler != null) {
                bridgeHandler.sendCallTimeOut();
            }
        }
    }

    /**
     * 通话超时
     */
    public void onCallCancel(boolean isSelfCall) {
        if (!isSelfCall) {
            if (bridgeHandler != null) {
                bridgeHandler.sendCallCancel();
            }
        }
    }

    private void handleWindowClosing() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要结束通话吗？", "结束通话",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            sendHangupSignal();
            closeWindow();
        }
    }

    private void sendHangupSignal() {
        try {
            SignalingMessage hangupMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(deviceInfo.getDeviceId())
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_HANGUP)
                    .setTimestamp(System.currentTimeMillis())
                    .setHangup(CallHangup.newBuilder()
                            .setReason("USER_HANGUP")
                            .setHangupType(CallHangup.HangupType.USER_HANGUP)
                            .setInitiatedBy(getCurrentUserId())
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(hangupMessage);
            logger.info("发送挂断信令: sessionId={}", sessionId);

        } catch (Exception e) {
            logger.error("发送挂断信令失败", e);
        }
    }

    @Override
    public void closeWindow() {
        cleanup();
        dispose();
    }

    @Override
    protected void cleanup() {
        // 调用父类清理
        super.cleanup();

        logger.info("通话窗口清理完成: sessionId={}", sessionId);
    }

    private void updateWindowTitle(String status) {
        setTitle(String.format("%s - 与%s (%s)", getMediaTypeName(), targetUserName, status));
    }

    // Getter方法
    public long getTargetUserId() {
        return targetUserId;
    }

    public boolean isIncomingCall() {
        return isIncomingCall;
    }
}