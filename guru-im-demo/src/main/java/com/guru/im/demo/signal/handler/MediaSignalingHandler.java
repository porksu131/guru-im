package com.guru.im.demo.signal.handler;

import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.protocol.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MediaSignalingHandler implements SignalingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MediaSignalingHandler.class);
    private final SignalingManager signalingManager;

    public MediaSignalingHandler(SignalingManager signalingManager) {
        this.signalingManager = signalingManager;
    }

    @Override
    public Set<SignalingType> getSupportedTypes() {
        return Set.of(
                SignalingType.MEDIA_CONTROL,
                SignalingType.STATE_SYNC
        );
    }

    @Override
    public void handle(SignalingMessage message) {
        switch (message.getType()) {
            case MEDIA_CONTROL:
                handleMediaControl(message);
                break;
            default:
                logger.warn("未处理的信令类型: {}", message.getType());
        }
    }

    private void handleMediaControl(SignalingMessage message) {
        MediaControl mediaControl = message.getMediaControl();

        logger.info("处理媒体控制: sessionId={}, controlType={}, targetUser={}",
                mediaControl.getSessionId(), mediaControl.getControlType(),
                mediaControl.getTargetUser());

        // 转发到对应的浏览器窗口
        signalingManager.forwardToBrowser(message.getSessionId(), message);
    }
}