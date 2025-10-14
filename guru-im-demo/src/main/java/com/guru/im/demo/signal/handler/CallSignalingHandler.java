package com.guru.im.demo.signal.handler;

import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;

import java.util.Set;

/**
 * 通话信令处理器
 */
public class CallSignalingHandler implements SignalingHandler {
    private final SignalingManager signalingManager;

    public CallSignalingHandler(SignalingManager signalingManager) {
        this.signalingManager = signalingManager;
    }

    @Override
    public Set<SignalingType> getSupportedTypes() {
        return Set.of(
                SignalingType.CALL_REQUEST,
                SignalingType.CALL_ACCEPT,
                SignalingType.CALL_REJECT,
                SignalingType.CALL_HANGUP,
                SignalingType.CALL_TIMEOUT,
                SignalingType.CALL_CANCEL,
                SignalingType.RINGING
        );
    }

    @Override
    public void handle(SignalingMessage message) {
        switch (message.getType()) {
            case CALL_REQUEST:
                handleCallRequest(message);
                break;
            case CALL_ACCEPT:
                handleCallAccept(message);
                break;
            case CALL_REJECT:
                handleCallReject(message);
                break;
            case CALL_HANGUP:
                handleCallHangup(message);
                break;
            case CALL_TIMEOUT:
                handleCallTimeOut(message);
                break;
            case CALL_CANCEL:
                handleCallCancel(message);
                break;
            case RINGING:
                handleRinging(message);
                break;
        }
    }

    private void handleCallRequest(SignalingMessage message) {
        // 处理来电
        signalingManager.getMediaWindowManager().handleIncomingCall(message);
    }

    private void handleCallAccept(SignalingMessage message) {
        // 处理通话接受
        signalingManager.getMediaWindowManager().handleCallAccepted(message.getSessionId(), false);
    }

    private void handleCallReject(SignalingMessage message) {
        // 处理通话拒绝
        signalingManager.getMediaWindowManager().handleCallRejected(message.getSessionId(), false);
    }

    private void handleCallHangup(SignalingMessage message) {
        // 处理通话挂断
        signalingManager.getMediaWindowManager().handleCallHangup(message.getSessionId(), false);
    }

    private void handleRinging(SignalingMessage message) {
        // 处理振铃
        // signalingManager.forwardToBrowser(message.getSessionId(), message);
    }

    private void handleCallTimeOut(SignalingMessage message) {
        signalingManager.getMediaWindowManager().handleCallTimeout(message.getSessionId(), false);
    }

    private void handleCallCancel(SignalingMessage message) {
        signalingManager.getMediaWindowManager().handleCallCancel(message.getSessionId(), false);
    }
}