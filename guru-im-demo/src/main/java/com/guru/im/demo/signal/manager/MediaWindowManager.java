package com.guru.im.demo.signal.manager;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.service.UserService;
import com.guru.im.demo.signal.bridge.UserSpecificJCEFManager;
import com.guru.im.demo.signal.windows.*;
import com.guru.im.protocol.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 音视频通话管理器
 */
public class MediaWindowManager {
    private static final Logger logger = LoggerFactory.getLogger(MediaWindowManager.class);
    private final Map<Long, VideoCallWindow> activeCalls; // sessionId -> 音视频通话窗口
    private final Map<Long, ConferenceWindow> activeConferences; // sessionId -> 会义窗口

    private boolean cefInitialized = false;

    private final MainFrame mainFrame;
    private final SignalingManager signalingManager;

    public MediaWindowManager(SignalingManager signalingManager, MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.signalingManager = signalingManager;
        this.activeCalls = new HashMap<>();
        this.activeConferences = new HashMap<>();
        initializeJCEF();
    }

    private void initializeJCEF() {
        try {
            // 使用新的JCEF管理器
            UserSpecificJCEFManager jcefManager = UserSpecificJCEFManager.getInstance(getCurrentUserId());
            jcefManager.initialize();
            cefInitialized = jcefManager.isInitialized();

            if (cefInitialized) {
                logger.info("JCEF初始化成功");
            } else {
                logger.warn("JCEF初始化失败，将无法使用音视频通话功能");
            }

        } catch (Exception e) {
            logger.error("JCEF初始化失败", e);
            cefInitialized = false;
        }
    }

    /**
     * 发起一对一语音通话
     */
    public void startAudioCall(long targetUserId, String targetUserName, MainFrame parentWindow) {
        if (!cefInitialized) {
            showCEFError(parentWindow);
            return;
        }

        try {
            // 申请有效的sessionId
            Long sessionId = initSession(targetUserId, MediaType.MEDIA_AUDIO_ONLY);

            AudioCallWindow callWindow = new AudioCallWindow(signalingManager, sessionId, targetUserId,
                    targetUserName, parentWindow, false);

            activeCalls.put(sessionId, callWindow);

            // 显示通话窗口
            callWindow.showWindow();

            logger.info("创建语音通话窗口，等待设备就绪: sessionId={}", sessionId);

        } catch (Exception e) {
            logger.error("启动语音通话失败", e);
            showErrorDialog(parentWindow, "启动语音通话失败: " + e.getMessage());
        }
    }

    /**
     * 发起一对一视频通话
     */
    public void startVideoCall(long targetUserId, String targetUserName, MainFrame parentWindow) {
        if (!cefInitialized) {
            showCEFError(parentWindow);
            return;
        }

        try {
            // 申请有效的sessionId
            Long sessionId = initSession(targetUserId, MediaType.MEDIA_VIDEO);

            VideoCallWindow callWindow = new VideoCallWindow(signalingManager, sessionId, targetUserId,
                    targetUserName, MediaType.MEDIA_VIDEO, parentWindow, false);

            activeCalls.put(sessionId, callWindow);

            callWindow.showWindow();

            logger.info("创建视频通话窗口，等待设备就绪: sessionId={}", sessionId);

        } catch (Exception e) {
            logger.error("启动视频通话失败", e);
            showErrorDialog(parentWindow, "启动视频通话失败: " + e.getMessage());
        }
    }

    /**
     * 发起一对一会议
     */
    public void startConference(long targetUserId, String targetUserName, MainFrame parentWindow) {
        if (!cefInitialized) {
            showCEFError(parentWindow);
            return;
        }

        try {
            // 申请有效的sessionId
            Long sessionId = initSession(targetUserId, MediaType.MEDIA_SCREEN_SHARE);
            ScreenWindow callWindow = new ScreenWindow(signalingManager, sessionId, targetUserId,
                    targetUserName, parentWindow, false);

            activeCalls.put(sessionId, callWindow);

            callWindow.showWindow();
            logger.info("启动屏幕共享窗口，等待设备就绪: sessionId={}", sessionId);

        } catch (Exception e) {
            logger.error("启动屏幕共享窗口失败", e);
            showErrorDialog(parentWindow, "启动屏幕共享窗口失败: " + e.getMessage());
        }
    }


    /**
     * 发起群组会议
     */
    public void startGroupConference(long[] participantIds, String[] participantNames,
                                     String conferenceTitle, MainFrame parentWindow) {
        if (!cefInitialized) {
            showCEFError(parentWindow);
            return;
        }

        try {
            ConferenceWindow conferenceWindow = new ConferenceWindow(
                    signalingManager,
                    participantIds,
                    participantNames,
                    conferenceTitle,
                    parentWindow,
                    false
            );
            conferenceWindow.showWindow();

            long conferenceId = conferenceWindow.getConferenceId();
            activeConferences.put(conferenceId, conferenceWindow);

            // 发送会议邀请
            sendConferenceInvite(conferenceId, participantIds, conferenceTitle);

            logger.info("发起会议: conferenceId={}, title={}, participants={}",
                    conferenceId, conferenceTitle, Arrays.toString(participantIds));

        } catch (Exception e) {
            logger.error("启动会议失败", e);
            showErrorDialog(parentWindow, "启动会议失败: " + e.getMessage());
        }
    }

    /**
     * 会话初始化
     */
    public Long initSession(long targetUserId, MediaType mediaType) {
        SignalingMessage initRequest = SignalingMessage.newBuilder()
                .setFromUser(getCurrentUserId())
                .setFromDevice(mainFrame.getDeviceInfo().getDeviceId())
                .addToUsers(targetUserId)
                .setType(SignalingType.INIT_SESSION)
                .setTimestamp(System.currentTimeMillis())
                .setCallRequest(CallRequest.newBuilder()
                        .setMediaType(mediaType)
                        .addTargetUsers(targetUserId)
                        .setTimeoutSeconds(30)
                        .build())
                .build();
        SignalMessageAck signalMessageAck = signalingManager.sendWithAck(initRequest, 10, TimeUnit.SECONDS);
        return signalMessageAck.getSessionId();
    }


    /**
     * 处理设备就绪通知 - 由浏览器调用
     */
    public void onDevicesReady(long sessionId, long targetUserId, MediaType mediaType) {

    }

    /**
     * 处理设备初始化失败
     */
    public void onDevicesFailed(long sessionId, String error) {
        logger.warn("设备初始化失败: sessionId={}, error={}", sessionId, error);

        // 通知窗口显示错误
        VideoCallWindow callWindow = activeCalls.get(sessionId);
        if (callWindow != null) {
            // 可以通过信令通知浏览器显示错误界面
            callWindow.showError("设备初始化失败: " + error);
        }
    }

    /**
     * 处理来电
     */
    public void handleIncomingCall(SignalingMessage signalingMessage) {
        if (!cefInitialized) {
            logger.warn("CEF未初始化，无法处理来电");
            return;
        }

        long sessionId = signalingMessage.getSessionId();
        long fromUser = signalingMessage.getFromUser();
        MediaType mediaType = signalingMessage.getCallRequest().getMediaType();

        // 检查是否已有通话窗口
        if (activeCalls.containsKey(sessionId)) {
            logger.warn("重复的来电会话: {}", sessionId);
            return;
        }

        // 获取呼叫者名称
        String callerName = getCallerName(fromUser);

        // 打开来电窗口
        mainFrame.getMediaWindowManager().startIncomingCall(sessionId, fromUser, callerName, mediaType, mainFrame);

        logger.info("显示来电窗口: sessionId={}, caller={}", sessionId, callerName);
    }

    /**
     * 启动接听来电
     */
    public void startIncomingCall(long sessionId, long callerId, String callerName, MediaType mediaType, MainFrame parentWindow) {
        try {
            VideoCallWindow callWindow = null;

            switch (mediaType) {
                case MEDIA_AUDIO_ONLY: {
                    callWindow = new AudioCallWindow(signalingManager, sessionId, callerId,
                            callerName, parentWindow, true);
                    break;
                }

                case MEDIA_VIDEO: {
                    callWindow = new VideoCallWindow(signalingManager, sessionId, callerId,
                            callerName, MediaType.MEDIA_VIDEO, parentWindow, true);
                    break;
                }
                case MEDIA_SCREEN_SHARE: {
                    callWindow = new ScreenWindow(signalingManager, sessionId, callerId,
                            callerName, parentWindow, true);
                }

            }


            activeCalls.put(sessionId, callWindow);

            if (callWindow != null) {
                callWindow.showWindow();
            }


            logger.info("接听来电: sessionId={}, caller={}", sessionId, callerName);

        } catch (Exception e) {
            logger.error("接听来电失败", e);
        }
    }

    /**
     * 处理通话接受
     */
    public void handleCallAccepted(long sessionId, boolean isSelfCall) {
        VideoCallWindow callWindow = activeCalls.get(sessionId);

        if (callWindow != null) {
            callWindow.onCallAccepted(isSelfCall);
            logger.info("通话被接受: sessionId={}", sessionId);
        } else {
            logger.warn("未找到对应的通话窗口: sessionId={}", sessionId);
        }
    }

    /**
     * 处理通话拒绝
     */
    public void handleCallRejected(long sessionId, boolean isSelfCall) {
        VideoCallWindow callWindow = activeCalls.get(sessionId);

        if (callWindow != null) {
            callWindow.onCallRejected(isSelfCall);
            activeCalls.remove(sessionId);
            logger.info("通话被拒绝: sessionId={}", sessionId);
        }
    }

    /**
     * 处理通话挂断
     */
    public void handleCallHangup(long sessionId, boolean isSelfCall) {
        VideoCallWindow callWindow = activeCalls.get(sessionId);

        if (callWindow != null) {
            callWindow.onCallHangup(isSelfCall);
            activeCalls.remove(sessionId);
            logger.info("处理通话挂断: sessionId={}", sessionId);
        }
    }

    /**
     * 处理呼叫超时
     */
    public void handleCallTimeout(long sessionId, boolean isSelfCall) {
        VideoCallWindow callWindow = activeCalls.get(sessionId);

        if (callWindow != null) {
            callWindow.onCallTimeout(isSelfCall);
            activeCalls.remove(sessionId);
            logger.info("处理通话超时: sessionId={}", sessionId);
        }
    }

    /**
     * 处理呼叫超时
     */
    public void handleCallCancel(long sessionId, boolean isSelfCall) {
        VideoCallWindow callWindow = activeCalls.get(sessionId);

        if (callWindow != null) {
            callWindow.onCallCancel(isSelfCall);
            activeCalls.remove(sessionId);
            logger.info("处理通话取消: sessionId={}", sessionId);
        }
    }

    /**
     * 处理会议邀请
     */
    public void handleConferenceInvite(SignalingMessage signalingMessage) {
        if (!cefInitialized) {
            logger.warn("CEF未初始化，无法处理会议邀请");
            return;
        }

        ConferenceInvite conferenceInvite = signalingMessage.getConferenceInvite();
        long conferenceId = conferenceInvite.getConferenceId();
        long fromUser = signalingMessage.getFromUser();

        // 检查是否已有会议窗口
        if (activeConferences.containsKey(conferenceId)) {
            logger.warn("重复的会议邀请: {}", conferenceId);
            return;
        }

        // 显示会议邀请对话框
        showConferenceInviteDialog(conferenceInvite, fromUser);
    }

    /**
     * 显示会议邀请对话框
     */
    private void showConferenceInviteDialog(ConferenceInvite conferenceInvite, long fromUser) {
        SwingUtilities.invokeLater(() -> {
            String callerName = getCallerName(fromUser);
            String conferenceTitle = conferenceInvite.getConferenceTitle();
            MediaType mediaType = conferenceInvite.getMediaType();

            int result = JOptionPane.showConfirmDialog(null,
                    String.format("用户 %s 邀请您参加会议：%s\n会议类型：%s",
                            callerName, conferenceTitle, getMediaTypeText(mediaType)),
                    "会议邀请",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                // 接受会议邀请
                acceptConferenceInvite(conferenceInvite, fromUser);
            } else {
                // 拒绝会议邀请
                rejectConferenceInvite(conferenceInvite, fromUser);
            }
        });
    }

    /**
     * 接受会议邀请
     */
    private void acceptConferenceInvite(ConferenceInvite conferenceInvite, long fromUser) {
        try {
            long conferenceId = conferenceInvite.getConferenceId();

            // 创建会议窗口
            ConferenceWindow conferenceWindow = new ConferenceWindow(
                    signalingManager,
                    new long[]{fromUser},
                    new String[]{getCallerName(fromUser)},
                    conferenceInvite.getConferenceTitle(),
                    null, // parentWindow
                    true  // isIncoming
            );

            activeConferences.put(conferenceId, conferenceWindow);
            conferenceWindow.showWindow();

            // 发送加入会议信令
            sendConferenceJoin(conferenceId, conferenceInvite.getPassword());

            logger.info("接受会议邀请: conferenceId={}", conferenceId);

        } catch (Exception e) {
            logger.error("接受会议邀请失败", e);
        }
    }

    /**
     * 拒绝会议邀请
     */
    private void rejectConferenceInvite(ConferenceInvite conferenceInvite, long fromUser) {
        try {
            SignalingMessage rejectMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(conferenceInvite.getConferenceId())
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(mainFrame.getDeviceInfo().getDeviceId())
                    .addToUsers(fromUser)
                    .setType(SignalingType.CONFERENCE_REJECT)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason("用户拒绝")
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(rejectMessage);

            logger.info("拒绝会议邀请: conferenceId={}", conferenceInvite.getConferenceId());

        } catch (Exception e) {
            logger.error("拒绝会议邀请失败", e);
        }
    }

    /**
     * 处理会议加入
     */
    public void handleConferenceJoin(SignalingMessage signalingMessage) {
        long conferenceId = signalingMessage.getSessionId();
        ConferenceWindow conferenceWindow = activeConferences.get(conferenceId);

        if (conferenceWindow != null) {
            conferenceWindow.onParticipantJoined(signalingMessage.getFromUser());
        }
    }

    /**
     * 处理会议离开
     */
    public void handleConferenceLeave(SignalingMessage signalingMessage) {
        long conferenceId = signalingMessage.getSessionId();
        ConferenceWindow conferenceWindow = activeConferences.get(conferenceId);

        if (conferenceWindow != null) {
            conferenceWindow.onParticipantLeft(signalingMessage.getFromUser());
        }
    }

    /**
     * 发送会议邀请信令
     */
    private void sendConferenceInvite(long conferenceId, long[] participantIds, String conferenceTitle) {
        try {
            SignalingMessage inviteMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(conferenceId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(mainFrame.getDeviceInfo().getDeviceId())
                    .addAllToUsers(Arrays.stream(participantIds).boxed().collect(Collectors.toList()))
                    .setType(SignalingType.CONFERENCE_INVITE)
                    .setTimestamp(System.currentTimeMillis())
                    .setConferenceInvite(ConferenceInvite.newBuilder()
                            .setConferenceId(conferenceId)
                            .setConferenceTitle(conferenceTitle)
                            .setMediaType(MediaType.MEDIA_VIDEO) // 默认视频会议
                            .addAllInvitees(Arrays.stream(participantIds).boxed().collect(Collectors.toList()))
                            .setHostUser(getCurrentUserId())
                            .setExpiresAt(System.currentTimeMillis() + 3600000) // 1小时过期
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(inviteMessage);

        } catch (Exception e) {
            logger.error("发送会议邀请失败", e);
            throw new RuntimeException("发送会议邀请失败", e);
        }
    }

    /**
     * 发送加入会议信令
     */
    private void sendConferenceJoin(long conferenceId, String password) {
        try {
            SignalingMessage joinMessage = SignalingMessage.newBuilder()
                    .setMessageId(System.currentTimeMillis())
                    .setSessionId(conferenceId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(mainFrame.getDeviceInfo().getDeviceId())
                    .setType(SignalingType.CONFERENCE_JOIN)
                    .setTimestamp(System.currentTimeMillis())
                    .setConferenceJoin(ConferenceJoin.newBuilder()
                            .setConferenceId(conferenceId)
                            .setPassword(password != null ? password : "")
                            .setInitialMediaState(MediaState.newBuilder()
                                    .setAudioEnabled(true)
                                    .setVideoEnabled(true)
                                    .setScreenShareEnabled(false)
                                    .build())
                            .setIsRejoin(false)
                            .build())
                    .build();

            signalingManager.forwardToNettyAsync(joinMessage);

        } catch (Exception e) {
            logger.error("发送加入会议信令失败", e);
            throw new RuntimeException("发送加入会议信令失败", e);
        }
    }

    /**
     * 关闭会议窗口
     */
    public void closeConferenceWindow(long conferenceId) {
        ConferenceWindow conferenceWindow = activeConferences.remove(conferenceId);
        if (conferenceWindow != null) {
            conferenceWindow.closeWindow();
            logger.info("关闭会议窗口: conferenceId={}", conferenceId);
        }
    }

    /**
     * 处理会议踢出
     */
    public void handleConferenceKick(SignalingMessage signalingMessage) {
        ConferenceKick kick = signalingMessage.getConferenceKick();
        long conferenceId = kick.getConferenceId();
        long kickedUser = kick.getKickedUser();

        // 如果是当前用户被踢出，关闭窗口
        if (kickedUser == getCurrentUserId()) {
            closeConferenceWindow(conferenceId);
            showErrorDialog(null, "您已被主持人移出会议");
        }
    }

    /**
     * 处理会议更新
     */
    public void handleConferenceUpdate(SignalingMessage signalingMessage) {
        long conferenceId = signalingMessage.getSessionId();
        ConferenceWindow conferenceWindow = activeConferences.get(conferenceId);

        if (conferenceWindow != null) {
            conferenceWindow.handleSignalingMessage(signalingMessage);
        }
    }

    /**
     * 获取媒体类型文本
     */
    private String getMediaTypeText(MediaType mediaType) {
        switch (mediaType) {
            case MEDIA_VIDEO:
                return "视频会议";
            case MEDIA_AUDIO_ONLY:
                return "语音会议";
            case MEDIA_SCREEN_SHARE:
                return "屏幕共享会议";
            case MEDIA_GROUP_VIDEO:
                return "群组视频会议";
            default:
                return "未知类型";
        }
    }

    private String getCallerName(long userId) {
        return UserService.getUserName(userId);
    }

    private long getCurrentUserId() {
        return mainFrame.getCurrentUser().getUid();
    }

    private void showCEFError(JFrame parentWindow) {
        showErrorDialog(parentWindow, "功能不可用，音视频组件初始化失败，无法使用通话功能。\n请检查JCEF依赖是否正确配置。");
    }

    void showErrorDialog(JFrame parentWindow, String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parentWindow, message, "错误", JOptionPane.ERROR_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(parentWindow, message, "错误", JOptionPane.ERROR_MESSAGE));
        }
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        // 清理所有通话窗口
        activeCalls.values().forEach(VideoCallWindow::closeWindow);
        activeCalls.clear();

        // 清理会议窗口
        activeConferences.values().forEach(ConferenceWindow::closeWindow);
        activeConferences.clear();

        // 关闭JCEF资源
        UserSpecificJCEFManager.getInstance(getCurrentUserId()).shutdown();

        logger.info("MediaWindowManager已关闭");
    }

    public boolean isCEFInitialized() {
        return cefInitialized;
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public boolean hasMediaWindow(long sessionId) {
        boolean existCall = activeCalls.containsKey(sessionId);
        if (!existCall) {
            return activeConferences.containsKey(sessionId);
        }
        return true;
    }

    public Map<Long, VideoCallWindow> getActiveCalls() {
        return activeCalls;
    }

    public AbstractMediaWindow getActiveMediaWindow(long sessionId) {
        VideoCallWindow videoCallWindow = activeCalls.get(sessionId);
        if (videoCallWindow == null) {
            return activeConferences.get(sessionId);
        }
        return videoCallWindow;
    }
}