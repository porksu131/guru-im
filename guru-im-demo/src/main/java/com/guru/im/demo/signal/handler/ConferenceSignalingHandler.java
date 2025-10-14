package com.guru.im.demo.signal.handler;

import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.protocol.model.ConferenceKick;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 会议信令处理器
 */
public class ConferenceSignalingHandler implements SignalingHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceSignalingHandler.class);
    private final SignalingManager signalingManager;

    public ConferenceSignalingHandler(SignalingManager signalingManager) {
        this.signalingManager = signalingManager;
    }

    @Override
    public Set<SignalingType> getSupportedTypes() {
        return Set.of(
                SignalingType.CONFERENCE_INVITE,
                SignalingType.CONFERENCE_JOIN,
                SignalingType.CONFERENCE_LEAVE,
                SignalingType.CONFERENCE_KICK,
                SignalingType.CONFERENCE_UPDATE,
                SignalingType.CONFERENCE_REJECT,
                SignalingType.CONFERENCE_ENDED
        );
    }

    @Override
    public void handle(SignalingMessage message) {
        switch (message.getType()) {
            case CONFERENCE_INVITE:
                handleConferenceInvite(message);
                break;
            case CONFERENCE_JOIN:
                handleConferenceJoin(message);
                break;
            case CONFERENCE_LEAVE:
                handleConferenceLeave(message);
                break;
            case CONFERENCE_KICK:
                handleConferenceKick(message);
                break;
            case CONFERENCE_UPDATE:
                handleConferenceUpdate(message);
                break;
            case CONFERENCE_REJECT:
                handleConferenceReject(message);
                break;
            case CONFERENCE_ENDED:
                handleConferenceEnded(message);
                break;
        }
    }

    private void handleConferenceInvite(SignalingMessage message) {
        // 处理会议邀请
        signalingManager.getMediaWindowManager().handleConferenceInvite(message);
    }

    private void handleConferenceJoin(SignalingMessage message) {
        long conferenceId = message.getSessionId();
        long joiningUser = message.getFromUser();

        // 通知对应的会议窗口有新参与者加入
        signalingManager.forwardToBrowser(conferenceId, message);

        // 调用MediaWindowManager处理参与者加入
        signalingManager.getMediaWindowManager().handleConferenceJoin(message);

        logger.info("用户 {} 加入会议: {}", joiningUser, conferenceId);
    }

    private void handleConferenceLeave(SignalingMessage message) {
        long conferenceId = message.getSessionId();
        long leavingUser = message.getFromUser();

        // 通知对应的会议窗口有参与者离开
        signalingManager.forwardToBrowser(conferenceId, message);

        // 调用MediaWindowManager处理参与者离开
        signalingManager.getMediaWindowManager().handleConferenceLeave(message);

        logger.info("用户 {} 离开会议: {}", leavingUser, conferenceId);
    }

    private void handleConferenceKick(SignalingMessage message) {
        ConferenceKick kick = message.getConferenceKick();
        long conferenceId = kick.getConferenceId();
        long kickedUser = kick.getKickedUser();

        // 调用MediaWindowManager处理踢出参与者
        signalingManager.getMediaWindowManager().handleConferenceKick(message);

        // 如果是自己被踢出，关闭窗口
        if (kickedUser == getCurrentUserId()) {
            signalingManager.getMediaWindowManager().closeConferenceWindow(conferenceId);
        }

        signalingManager.forwardToBrowser(conferenceId, message);
    }

    private void handleConferenceUpdate(SignalingMessage message) {
        // 处理会议更新（标题变更、主持人变更等）
        signalingManager.getMediaWindowManager().handleConferenceUpdate(message);
        signalingManager.forwardToBrowser(message.getSessionId(), message);
    }

    private void handleConferenceReject(SignalingMessage message) {
        // 处理会议邀请被拒绝
        long conferenceId = message.getSessionId();
        logger.info("会议邀请被拒绝: {}", conferenceId);
    }

    private void handleConferenceEnded(SignalingMessage message) {
        // 处理会议结束
        long conferenceId = message.getSessionId();
        signalingManager.getMediaWindowManager().closeConferenceWindow(conferenceId);
    }

    private long getCurrentUserId() {
        // 获取当前用户ID
        return signalingManager.getMediaWindowManager().getMainFrame().getCurrentUser().getUid();
    }
}