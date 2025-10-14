package com.guru.im.demo.signal.windows;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.signal.model.MediaDataClasses;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.demo.util.ImageUtil;
import com.guru.im.protocol.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 会议窗口
 */
public class ConferenceWindow extends AbstractMediaWindow {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceWindow.class);
    private final long[] participantIds;
    private final String[] participantNames;
    private String conferenceTitle;
    private final long conferenceId;
    private final boolean isIncoming;
    private final MediaDataClasses.ConferenceInitData conferenceInitData;

    private final Map<Long, String> participants; // userId -> userName

    public ConferenceWindow(SignalingManager signalingManager, long[] participantIds,
                            String[] participantNames, String conferenceTitle,
                            MainFrame parentWindow, boolean isIncoming) {
        super(signalingManager, MediaType.MEDIA_GROUP_VIDEO, parentWindow, System.currentTimeMillis());

        this.participantIds = participantIds != null ? participantIds.clone() : new long[0];
        this.participantNames = participantNames != null ? participantNames.clone() : new String[0];
        this.conferenceTitle = conferenceTitle != null ? conferenceTitle : "会议";
        this.isIncoming = isIncoming;
        this.conferenceId = this.sessionId; // 使用父类的sessionId作为conferenceId
        this.participants = new HashMap<>();

        initializeParticipants();

        // 创建会议初始化数据
        this.conferenceInitData = new MediaDataClasses.ConferenceInitData(
                conferenceId, conferenceTitle, getCurrentUserId(),
                currentUser, deviceInfo, participants, isIncoming
        );

        initializeUI();
        initializeBrowser(conferenceInitData);
    }

    private void initializeParticipants() {
        for (int i = 0; i < participantIds.length; i++) {
            participants.put(participantIds[i],
                    i < participantNames.length ? participantNames[i] : getUserName(participantIds[i]));
        }
        // 添加自己
        participants.put(getCurrentUserId(), "我");
    }

    private void initializeUI() {
        setTitle(String.format("会议 - %s (%d人)", conferenceTitle, participants.size()));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(parentWindow);
        // 设置窗口图标
        ImageIcon icon = ImageUtil.createSVGIcon("image/screen.svg", 16);
        if (icon != null) {
            setIconImage(icon.getImage());
        }
        addCommonWindowListener(this::handleWindowClosing);
    }

    private void initializeBrowser(MediaDataClasses.ConferenceInitData initData) {
        initializeBrowser("conference-call.html", initData); // 会议视图
    }

    /**
     * 处理参与者加入
     */
    public void onParticipantJoined(long userId) {
        String userName = getUserName(userId);
        participants.put(userId, userName);

        // 更新窗口标题
        updateWindowTitle();

        // 通知浏览器
        if (bridgeHandler != null) {
            bridgeHandler.sendParticipantJoined(userId, userName);
        }

        logger.info("参与者加入会议: conferenceId={}, userId={}, userName={}",
                conferenceId, userId, userName);
    }

    /**
     * 处理参与者离开
     */
    public void onParticipantLeft(long userId) {
        String userName = participants.remove(userId);

        // 更新窗口标题
        updateWindowTitle();

        // 通知浏览器
        if (bridgeHandler != null) {
            bridgeHandler.sendParticipantLeft(userId);
        }

        logger.info("参与者离开会议: conferenceId={}, userId={}, userName={}",
                conferenceId, userId, userName);
    }

    private void updateWindowTitle() {
        setTitle(String.format("会议 - %s (%d人)", conferenceTitle, participants.size()));
    }

    private void handleWindowClosing() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要离开会议吗？", "离开会议",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            sendLeaveConferenceSignal();
            closeWindow();
        }
    }

    /**
     * 处理会议更新信令
     */
    public void handleConferenceUpdate(SignalingMessage message) {
        ConferenceUpdate update = message.getConferenceUpdate();

        switch (update.getUpdateType()) {
            case TITLE_CHANGE:
                updateConferenceTitle(update.getConferenceTitle());
                break;
            case HOST_CHANGE:
                updateHost(update.getNewHost());
                break;
            case MEDIA_TYPE_CHANGE:
                updateMediaType(update.getMediaType());
                break;
        }

        // 通知浏览器
        if (bridgeHandler != null) {
            bridgeHandler.sendSignalingMessage(message);
        }
    }

    /**
     * 更新会议标题
     */
    private void updateConferenceTitle(String newTitle) {
        this.conferenceTitle = newTitle;
        updateWindowTitle();

        SwingUtilities.invokeLater(() -> {
            setTitle(String.format("会议 - %s (%d人)", conferenceTitle, participants.size()));
        });
    }

    /**
     * 更新主持人
     */
    private void updateHost(long newHostId) {
        // 更新主持人逻辑
        logger.info("会议主持人变更为: {}", newHostId);

        // 通知浏览器
        if (bridgeHandler != null) {
            // 发送主持人变更事件到浏览器
        }
    }

    /**
     * 更新媒体类型
     */
    private void updateMediaType(MediaType mediaType) {
        logger.info("会议媒体类型变更为: {}", mediaType);
        // 处理媒体类型变更逻辑
    }

    /**
     * 踢出参与者
     */
    public void kickParticipant(long userId) {
        try {
            SignalingMessage kickMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(conferenceId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice("java-client")
                    .setType(SignalingType.CONFERENCE_KICK)
                    .setTimestamp(System.currentTimeMillis())
                    .setConferenceKick(ConferenceKick.newBuilder()
                            .setConferenceId(conferenceId)
                            .setKickedUser(userId)
                            .setOperatorUser(getCurrentUserId())
                            .setReason("主持人踢出")
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(kickMessage);
            logger.info("踢出参与者: conferenceId={}, userId={}", conferenceId, userId);

        } catch (Exception e) {
            logger.error("踢出参与者失败", e);
        }
    }

    private void sendLeaveConferenceSignal() {
        try {
            SignalingMessage leaveMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(conferenceId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(parentWindow.getDeviceInfo().getDeviceId())
                    .setType(SignalingType.CONFERENCE_LEAVE)
                    .setTimestamp(System.currentTimeMillis())
                    .setConferenceLeave(ConferenceLeave.newBuilder()
                            .setConferenceId(conferenceId)
                            .setReason(ConferenceLeave.LeaveReason.USER_LEFT)
                            .setDuration(0) // 可以根据实际时长计算
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(leaveMessage);

        } catch (Exception e) {
            logger.error("发送离开会议信令失败", e);
        }
    }

    @Override
    protected void cleanup() {
        // 调用父类清理
        super.cleanup();

        logger.info("会议窗口清理完成: conferenceId={}", conferenceId);
    }

    // Getter方法
    public long getConferenceId() {
        return conferenceId;
    }

    public String getConferenceTitle() {
        return conferenceTitle;
    }

    public Map<Long, String> getParticipants() {
        return new HashMap<>(participants);
    }

}