package com.guru.im.signal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.protocol.model.*;
import com.guru.im.signal.builder.SignalingMessageBuilder;
import com.guru.im.signal.model.pojo.CallParticipant;
import com.guru.im.signal.model.pojo.CallSession;
import com.guru.im.signal.model.pojo.MediaRoomMapping;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SignalingMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(SignalingMessageHandler.class);
    @Autowired
    private CallSessionService callSessionService;

    @Autowired
    private MediaRoomServiceImpl mediaRoomService;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SignalingMessageBuilder messageBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    public void handleSignalingMessage(SignalingMessage signaling) {
        SignalingType type = signaling.getType();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        log.info("处理信令消息: type={}, sessionId={}, fromUser={}", type, sessionId, fromUser);

        try {
            switch (type) {
                // 会话初始化
                case INIT_SESSION:
                    handleInitSession(signaling);
                    break;
                case JOIN_ROOM:
                    handleJoinRoom(signaling);
                    break;
                // 呼叫相关
                case CALL_REQUEST:
                    handleCallRequest(signaling);
                    break;
                case CALL_ACCEPT:
                    handleCallAccept(signaling);
                    break;
                case CALL_REJECT:
                    handleCallReject(signaling);
                    break;
                case CALL_CANCEL:
                    handleCallCancel(signaling);
                    break;
                case CALL_HANGUP:
                    handleCallHangup(signaling);
                    break;
                case CALL_TIMEOUT:
                    handleCallTimeout(signaling);
                    break;
                case RINGING:
                    handleRinging(signaling);
                    break;

                // 会议相关
                case CONFERENCE_INVITE:
                    handleConferenceInvite(signaling);
                    break;
                case CONFERENCE_JOIN:
                    handleConferenceJoin(signaling);
                    break;
                case CONFERENCE_LEAVE:
                    handleConferenceLeave(signaling);
                    break;

                // 每条控制相关
                case MEDIA_CONTROL:
                    handleMediaControl(signaling);
                    break;
                case STATE_SYNC:
                    handleStateSync(signaling);
                    break;

                // mediasoup相关信令处理
                case MEDIASOUP_TRANSPORT_CREATE:
                    handleTransportCreate(signaling);
                    break;
                case MEDIASOUP_TRANSPORT_CONNECT:
                    handleTransportConnect(signaling);
                    break;
                case MEDIASOUP_PRODUCE:
                    handleProduce(signaling);
                    break;
                case MEDIASOUP_CONSUME:
                    handleConsume(signaling);
                    break;
                case MEDIASOUP_CONSUMER_RESUME:
                    handleConsumerResume(signaling);
                    break;
                case RTP_CAPABILITIES_REQUEST:
                    handleRtpCapabilitiesRequest(signaling);
                    break;
                case ROOM_PRODUCERS_REQUEST:
                    handleRoomProducersRequest(signaling);
                    break;
                default:
                    log.warn("未知的信令类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理信令消息失败: type={}, sessionId={}", type, sessionId, e);
            // 发送错误响应
            sendErrorResponse(signaling, e.getMessage());
        }
    }

    private void handleJoinRoom(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();
        String roomId = "room_" + sessionId;
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();
        long requestMessageId = signaling.getMessageId();
        try {
            CallSession callSession = callSessionService.getCallSession(signaling.getSessionId());
            if (callSession == null) {
                throw new RuntimeException("会话不存在，无法加入");
            }

            // 加入mediasoup房间
            Map<String, Object> rtpCapabilities = mediaRoomService.joinMediaRoom(sessionId, buildPeerId(fromUser, fromDevice));

            // 响应
            String rtpCapabilitiesJson = objectMapper.writeValueAsString(rtpCapabilities);

            SignalingMessage responseMsg = messageBuilder.buildRtpCapabilitiesResponse(
                    sessionId, fromUser, fromDevice, roomId, rtpCapabilitiesJson, requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);


        } catch (Exception e) {
            log.error("处理会话初始化失败: fromUser={}", fromUser, e);
            SignalingMessage errorMsg = messageBuilder.buildCallRequestResponse(
                    sessionId, fromUser, fromDevice, "", false, e.getMessage(), signaling.getMessageId()
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
        }
    }

    /**
     * 处理会话初始化
     */
    private void handleInitSession(SignalingMessage signaling) {
        CallRequest callRequest = signaling.getCallRequest();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            // 检查用户是否已有活跃通话（在所有设备上）
            List<CallSession> activeSessions = callSessionService.getUserActiveSessions(fromUser);
            if (CollectionUtils.isNotEmpty(activeSessions)) {
                activeSessions.forEach(obj -> callSessionService.updateCallState(obj.getId(), CallSession.CallState.ENDED));
                // throw new RuntimeException("用户已有活跃通话，无法发起新通话");todo
            }

            // 创建通话会话
            CallSession session = callSessionService.createCallSession(sessionId, fromUser, fromDevice, callRequest);

        } catch (Exception e) {
            log.error("处理会话初始化失败: fromUser={}", fromUser, e);
            SignalingMessage errorMsg = messageBuilder.buildCallRequestResponse(
                    sessionId, fromUser, fromDevice, "", false, e.getMessage(), signaling.getMessageId()
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
        }
    }

    /**
     * 处理呼叫请求
     */
    private void handleCallRequest(SignalingMessage signaling) {
        CallRequest callRequest = signaling.getCallRequest();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();
        Long requestMessageId = signaling.getMessageId();

        try {
            // 获取当前会话信息
            CallSession session = callSessionService.getCallSession(sessionId);
            if (session == null) {
                log.warn("通话会话不存在: sessionId={}", sessionId);
                return;
            }

            // 创建mediasoup房间
            MediaRoomMapping mediaRoom = mediaRoomService.createMediaRoom(session.getId(), buildPeerId(fromUser, fromDevice));

            // 添加目标用户为参与者
            for (Long toUser : signaling.getToUsersList()) {
                if (!toUser.equals(fromUser)) {
                    List<String> targetDevices = deviceService.getUserOnlineDevices(toUser);
                    for (String deviceId : targetDevices) {
                        callSessionService.addParticipant(session.getId(), toUser, deviceId, false);
                    }
                }
            }

            // 发送呼叫请求响应（成功）给发送方
            SignalingMessage responseMsg = messageBuilder.buildCallRequestResponse(
                    session.getId(), fromUser, fromDevice,
                    mediaRoom.getRoomId(), true, null, requestMessageId
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            // 向目标用户发送呼叫请求（转发）
            SignalingMessage inviteMsg = messageBuilder.buildCallInviteMessage(
                    session, signaling.getToUsersList(), callRequest
            );
            messageSender.broadcastToParticipants(session.getId(), inviteMsg, fromUser);

            log.info("呼叫请求处理成功: sessionId={}, initiator={}, targets={}",
                    session.getId(), fromUser, signaling.getToUsersList());

            // 启动设备选择和振铃流程
            startDeviceSelectionAndRinging(session.getId(), signaling.getToUsersList());

            // 更新会话状态
            callSessionService.updateCallState(session.getId(), CallSession.CallState.RINGING);

        } catch (Exception e) {
            log.error("处理呼叫请求失败: fromUser={}", fromUser, e);

            // 发送呼叫请求响应（失败）
            SignalingMessage errorMsg = messageBuilder.buildCallRequestResponse(
                    0L, fromUser, fromDevice, "", false, e.getMessage(), requestMessageId
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
        }
    }

    private void handleCallAccept(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 更新参与者状态为已加入
        callSessionService.updateParticipantState(
                sessionId, fromUser, fromDevice, CallParticipant.ParticipantState.JOINED
        );

        // 检查是否所有参与者都已接受，更新通话状态
        if (callSessionService.allParticipantsAccepted(sessionId)) {
            callSessionService.updateCallState(sessionId, CallSession.CallState.ACTIVE);

            // 获取所有参与者信息
            List<CallParticipant> participants = callSessionService.getSessionParticipants(sessionId);
            List<Participant> protoParticipants = convertToProtoParticipants(participants);

            // 获取所有参与者用户ID
            List<Long> participantUserIds = callSessionService.getParticipantUserIds(sessionId);

            // 通知所有参与者通话已开始
            SignalingMessage startMsg = messageBuilder.buildCallStartedMessage(
                    sessionId, participantUserIds, protoParticipants);
            messageSender.broadcastToParticipants(sessionId, startMsg);

            log.info("所有参与者已接受，通话开始: sessionId={}, participants={}",
                    sessionId, participantUserIds);
        } else {
            // 通知发起者有人接受了呼叫
            SignalingMessage acceptMsg = messageBuilder.buildCallAcceptMessage(
                    sessionId, fromUser, fromDevice, null);
            CallSession session = callSessionService.getCallSession(sessionId);
            messageSender.sendToUser(session.getInitiatorId(), acceptMsg);

            // 发送参与者状态更新
            Participant participant = buildParticipant(fromUser, fromDevice,
                    Participant.ParticipantState.JOINED, true);
            List<Long> otherParticipants = getOtherParticipants(sessionId, fromUser);

            if (!otherParticipants.isEmpty()) {
                SignalingMessage participantUpdateMsg = messageBuilder.buildParticipantUpdateMessage(
                        sessionId, otherParticipants, participant);
                messageSender.sendToUsers(otherParticipants, participantUpdateMsg);
            }
        }
    }

    private void handleCallReject(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 1. 更新参与者状态为已拒绝
        callSessionService.updateParticipantState(
                sessionId, fromUser, fromDevice, CallParticipant.ParticipantState.DECLINED
        );

        // 2. 通知发起者有人拒绝了呼叫
        SignalingMessage rejectMsg = messageBuilder.buildCallRejectMessage(sessionId, fromUser, fromDevice, "user rejected");
        CallSession session = callSessionService.getCallSession(sessionId);
        messageSender.sendToUser(session.getInitiatorId(), rejectMsg);

        log.info("用户拒绝通话: sessionId={}, userId={}", sessionId, fromUser);
    }

    private void handleCallCancel(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();
        String reason = "user cancelled";

        // 结束通话会话
        callSessionService.endCallSession(sessionId, reason,
                CallHangup.HangupType.USER_HANGUP_VALUE, fromUser);

        // 通知所有参与者通话已取消
        SignalingMessage cancelMsg = messageBuilder.buildCallCancelMessage(sessionId, fromUser, fromDevice, reason);
        messageSender.broadcastToParticipants(sessionId, cancelMsg);

        log.info("通话已取消: sessionId={}, userId={}", sessionId, fromUser);
    }

    private void handleCallHangup(SignalingMessage signaling) {
        CallHangup hangup = signaling.getHangup();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 结束通话会话
        callSessionService.endCallSession(sessionId, hangup.getReason(),
                hangup.getHangupType().getNumber(), fromUser);

        // 通知所有参与者通话已结束
        SignalingMessage hangupMsg = messageBuilder.buildCallHangupMessage(sessionId, fromUser, fromDevice, hangup);
        messageSender.broadcastToParticipants(sessionId, hangupMsg);

        // 清理媒体房间
        mediaRoomService.cleanupMediaRoom(sessionId);

        log.info("通话结束: sessionId={}, userId={}, reason={}",
                sessionId, fromUser, hangup.getReason());
    }

    /**
     * 处理呼叫超时
     */
    private void handleCallTimeout(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();

        try {
            CallSession session = callSessionService.getCallSession(sessionId);
            if (session == null) {
                log.warn("通话会话不存在: sessionId={}", sessionId);
                return;
            }

            // 结束通话会话
            callSessionService.endCallSession(
                    sessionId,
                    "呼叫超时",
                    CallHangup.HangupType.TIMEOUT_VALUE,
                    session.getInitiatorId()
            );

            // 通知所有参与者呼叫超时
            List<Long> participantUserIds = callSessionService.getParticipantUserIds(sessionId);

            SignalingMessage timeoutMsg = SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addAllToUsers(participantUserIds)
                    .setType(SignalingType.CALL_TIMEOUT)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(SignalingMessageBuilder.PROTOCOL_VERSION)
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason("呼叫超时")
                            .build())
                    .build();

            messageSender.broadcastToParticipants(sessionId, timeoutMsg);

            // 清理媒体房间
            mediaRoomService.cleanupMediaRoom(sessionId);

            log.info("处理呼叫超时: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("处理呼叫超时失败: sessionId={}", sessionId, e);
            throw new RuntimeException("处理呼叫超时失败: " + e.getMessage());
        }
    }

    private void handleRinging(SignalingMessage signaling) {
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 更新参与者状态为振铃中
        callSessionService.updateParticipantState(
                sessionId, fromUser, fromDevice, CallParticipant.ParticipantState.RINGING
        );

        log.info("用户开始振铃: sessionId={}, userId={}", sessionId, fromUser);
    }

    /**
     * 处理会议邀请 - 修改为返回响应
     */
    private void handleConferenceInvite(SignalingMessage signaling) {
        ConferenceInvite conferenceInvite = signaling.getConferenceInvite();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();
        Long requestMessageId = signaling.getMessageId();

        try {
            // 1. 创建会议会话
            CallSession session = callSessionService.createConferenceSession(
                    fromUser, fromDevice, conferenceInvite);

            // 2. 创建mediasoup房间
            MediaRoomMapping mediaRoom = mediaRoomService.createMediaRoom(session.getId(), buildPeerId(fromUser, fromDevice));

            // 3. 发送会议邀请响应（成功）
            SignalingMessage responseMsg = messageBuilder.buildConferenceInviteResponse(
                    session.getId(), fromUser, fromDevice,
                    mediaRoom.getRoomId(), true, null, requestMessageId
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            // 4. 向被邀请者发送会议邀请（转发）
            for (Long invitee : conferenceInvite.getInviteesList()) {
                if (!invitee.equals(fromUser)) {
                    callSessionService.addParticipant(session.getId(), invitee, "unknown", false);

                    SignalingMessage inviteMsg = messageBuilder.buildConferenceInviteMessage(
                            session, invitee, conferenceInvite
                    );
                    messageSender.sendToUser(invitee, inviteMsg);
                }
            }

            log.info("会议邀请处理成功: sessionId={}, host={}, invitees={}",
                    session.getId(), fromUser, conferenceInvite.getInviteesList());

        } catch (Exception e) {
            log.error("处理会议邀请失败: fromUser={}", fromUser, e);

            // 发送会议邀请响应（失败）
            SignalingMessage errorMsg = messageBuilder.buildConferenceInviteResponse(
                    0L, fromUser, fromDevice, "", false, e.getMessage(), requestMessageId
            );
            messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
        }
    }

    private void handleConferenceJoin(SignalingMessage signaling) {
        ConferenceJoin conferenceJoin = signaling.getConferenceJoin();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 1. 添加参与者（如果尚未添加）
        CallParticipant participant = callSessionService.getParticipant(sessionId, fromUser, fromDevice);
        if (participant == null) {
            callSessionService.addParticipant(sessionId, fromUser, fromDevice, false);
        }

        // 2. 更新参与者状态为已加入
        callSessionService.updateParticipantState(
                sessionId, fromUser, fromDevice, CallParticipant.ParticipantState.JOINED
        );

        // 3. 通知其他参与者有人加入会议
        SignalingMessage joinMsg = messageBuilder.buildConferenceJoinMessage(sessionId, fromUser, fromDevice, conferenceJoin);
        messageSender.broadcastToParticipants(sessionId, joinMsg, fromUser);

        log.info("用户加入会议: sessionId={}, userId={}", sessionId, fromUser);
    }

    private void handleConferenceLeave(SignalingMessage signaling) {
        ConferenceLeave conferenceLeave = signaling.getConferenceLeave();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        // 1. 更新参与者状态为已离开
        callSessionService.updateParticipantState(
                sessionId, fromUser, fromDevice, CallParticipant.ParticipantState.LEFT
        );

        // 2. 通知其他参与者有人离开会议
        SignalingMessage leaveMsg = messageBuilder.buildConferenceLeaveMessage(sessionId, fromUser, fromDevice, conferenceLeave);
        messageSender.broadcastToParticipants(sessionId, leaveMsg, fromUser);

        log.info("用户离开会议: sessionId={}, userId={}, reason={}",
                sessionId, fromUser, conferenceLeave.getReason());
    }

    private void handleMediaControl(SignalingMessage signaling) {
        MediaControl mediaControl = signaling.getMediaControl();
        Long sessionId = signaling.getSessionId();

        // 转发媒体控制消息给目标用户
        if (mediaControl.getTargetUser() > 0) {
            // 单用户控制
            SignalingMessage controlMsg = SignalingMessage.newBuilder(signaling)
                    .clearToUsers()
                    .addToUsers(mediaControl.getTargetUser())
                    .build();
            messageSender.sendMessage(controlMsg);
        } else {
            // 广播给所有参与者
            messageSender.broadcastToParticipants(sessionId, signaling, signaling.getFromUser());
        }

        log.info("媒体控制消息: sessionId={}, controlType={}, targetUser={}",
                sessionId, mediaControl.getControlType(), mediaControl.getTargetUser());
    }

    /**
     * 处理状态同步消息
     */
    private void handleStateSync(SignalingMessage signaling) {
        // 状态同步消息通常由服务器主动发送给客户端
        // 如果客户端发送状态同步，可能是请求状态更新或心跳
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();

        log.debug("收到状态同步消息: sessionId={}, fromUser={}", sessionId, fromUser);

        // 可以在这里处理客户端的状态同步请求
        // 比如返回当前通话的最新状态
        if (signaling.hasStateSync()) {
            CallStateSync stateSync = signaling.getStateSync();
            // 处理客户端发送的状态同步请求
            handleClientStateSync(sessionId, fromUser, stateSync);
        }
    }

    /**
     * 处理客户端状态同步请求
     */
    private void handleClientStateSync(Long sessionId, Long fromUser, CallStateSync stateSync) {
        try {
            // 获取当前通话的最新状态
            CallSession session = callSessionService.getCallSession(sessionId);
            if (session == null) {
                log.warn("通话会话不存在，无法同步状态: sessionId={}", sessionId);
                return;
            }

            List<CallParticipant> participants = callSessionService.getSessionParticipants(sessionId);
            List<Participant> protoParticipants = convertToProtoParticipants(participants);

            SignalingMessage response = messageBuilder.buildCallStateSyncMessage(
                    sessionId,
                    Collections.singletonList(fromUser),
                    CallStateSync.CallState.forNumber(session.getCallState()),
                    protoParticipants
            );

            messageSender.sendToUser(fromUser, response);

            log.debug("响应客户端状态同步请求: sessionId={}, userId={}", sessionId, fromUser);

        } catch (Exception e) {
            log.error("处理客户端状态同步失败: sessionId={}, userId={}", sessionId, fromUser, e);
        }
    }

    private List<Long> getOtherParticipants(Long sessionId, Long excludeUser) {
        return callSessionService.getParticipantUserIds(sessionId).stream()
                .filter(userId -> !userId.equals(excludeUser))
                .collect(Collectors.toList());
    }

    private void sendErrorResponse(SignalingMessage originalMsg, String error) {
        try {
            SignalingMessage errorMsg = SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(originalMsg.getSessionId())
                    .setFromUser(0L) // 系统用户
                    .setFromDevice("system")
                    .addToUsers(originalMsg.getFromUser())
                    .setType(SignalingType.CALL_REJECT)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason(error)
                            .build())
                    .build();

            messageSender.sendMessage(errorMsg);
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
        }
    }

    /**
     * 启动设备选择和振铃流程
     */
    private void startDeviceSelectionAndRinging(Long sessionId, List<Long> targetUsers) {
        for (Long targetUser : targetUsers) {
            try {
                // 延迟一段时间，等待设备上报能力
                scheduleDeviceSelection(sessionId, targetUser, 2000); // 2秒后选择设备
            } catch (Exception e) {
                log.error("启动设备选择失败: sessionId={}, targetUser={}", sessionId, targetUser, e);
            }
        }
    }

    /**
     * 调度设备选择
     */
    private void scheduleDeviceSelection(Long sessionId, Long targetUser, long delayMs) {
        // 使用定时任务或异步任务来延迟设备选择
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                selectAndRingDevice(sessionId, targetUser);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("设备选择调度被中断: sessionId={}, targetUser={}", sessionId, targetUser);
            }
        }).start();
    }

    /**
     * 选择并振铃设备
     */
    private void selectAndRingDevice(Long sessionId, Long targetUser) {
        try {
            // 获取用户的在线设备
            List<String> onlineDevices = deviceService.getUserOnlineDevices(targetUser);

            if (onlineDevices.isEmpty()) {
                log.warn("用户没有在线设备，无法振铃: sessionId={}, targetUser={}", sessionId, targetUser);

                // 发送呼叫失败通知
                SignalingMessage noDeviceMsg = messageBuilder.buildErrorResponseMessage(
                        sessionId, targetUser, null, "用户设备不在线", snowflakeIdGenerator.nextId());
                messageSender.sendToUser(targetUser, noDeviceMsg);
                return;
            }

            // 选择最佳振铃设备
            String selectedDevice = selectRingingDevice(targetUser, onlineDevices);

            if (selectedDevice != null) {
                // 发送振铃消息到选定设备
                Ringing ringing = Ringing.newBuilder()
                        .setDeviceId(selectedDevice)
                        .setRingStartTime(System.currentTimeMillis())
                        .addAllRingingDevices(onlineDevices)
                        .build();

                SignalingMessage ringingMsg = messageBuilder.buildRingingMessage(
                        sessionId, targetUser, selectedDevice, ringing);

                messageSender.sendToUserDevice(targetUser, selectedDevice, ringingMsg);

                // 更新参与者状态为振铃中
                callSessionService.updateParticipantState(
                        sessionId, targetUser, selectedDevice, CallParticipant.ParticipantState.RINGING
                );

                log.info("已发送振铃到选定设备: sessionId={}, userId={}, device={}",
                        sessionId, targetUser, selectedDevice);
            }

        } catch (Exception e) {
            log.error("选择振铃设备失败: sessionId={}, targetUser={}", sessionId, targetUser, e);
        }
    }

    /**
     * 选择振铃设备策略
     */
    private String selectRingingDevice(Long userId, List<String> onlineDevices) {
        if (onlineDevices.isEmpty()) {
            return null;
        }

        // 策略1: 优先选择用户主要设备
        String primaryDevice = deviceService.getUserPrimaryDevice(userId);
        if (primaryDevice != null && onlineDevices.contains(primaryDevice)) {
            return primaryDevice;
        }

        // 策略2: 根据设备类型选择（手机 > 平板 > 电脑）
        for (String device : onlineDevices) {
            if (device.contains("iphone") || device.contains("android")) {
                return device; // 优先选择手机
            }
        }
        for (String device : onlineDevices) {
            if (device.contains("ipad") || device.contains("tablet")) {
                return device; // 其次选择平板
            }
        }

        // 策略3: 选择第一个设备作为备选
        return onlineDevices.get(0);
    }

    /**
     * 将数据库参与者转换为Protobuf参与者
     */
    private List<Participant> convertToProtoParticipants(List<CallParticipant> dbParticipants) {
        return dbParticipants.stream()
                .map(this::convertToProtoParticipant)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个参与者
     */
    private Participant convertToProtoParticipant(CallParticipant dbParticipant) {
        Participant.Builder builder = Participant.newBuilder()
                .setUserId(dbParticipant.getUserId())
                .setDeviceId(dbParticipant.getDeviceId())
                .setState(Participant.ParticipantState.forNumber(dbParticipant.getParticipantState()))
                .setIsInviter(dbParticipant.getIsInviter() == 1);

        if (dbParticipant.getJoinTime() != null) {
            builder.setJoinTime(dbParticipant.getJoinTime());
        }

        if (dbParticipant.getMediaState() != null) {
            // 可以解析JSON格式的媒体状态
            // builder.setMediaState(parseMediaState(dbParticipant.getMediaState()));
        }

        return builder.build();
    }

    /**
     * 构建参与者对象
     */
    private Participant buildParticipant(Long userId, String deviceId,
                                         Participant.ParticipantState state, boolean isInviter) {
        return Participant.newBuilder()
                .setUserId(userId)
                .setDeviceId(deviceId)
                .setState(state)
                .setIsInviter(isInviter)
                .setJoinTime(System.currentTimeMillis())
                .build();
    }


    /**
     * 处理传输创建请求
     */
    private void handleTransportCreate(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        MediasoupTransportCreate transportCreate = signaling.getTransportCreate();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            // 获取媒体房间
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在: sessionId=" + sessionId);
            }

            // 创建WebRTC传输
            String peerId = buildPeerId(fromUser, fromDevice);
            Map<String, Object> transportData = mediaRoomService.createWebRtcTransport(
                    mediaRoom.getRoomId(), peerId);

            // 转换为JSON字符串
            String transportOptionsJson = objectMapper.writeValueAsString(transportData);

            // 发送传输创建响应
            SignalingMessage responseMsg = messageBuilder.buildTransportCreateResponse(
                    sessionId, fromUser, fromDevice, mediaRoom.getRoomId(), transportOptionsJson, requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            log.info("创建传输成功: sessionId={}, userId={}, device={}, transportId={}",
                    sessionId, fromUser, fromDevice, transportData.get("id"));

        } catch (Exception e) {
            log.error("处理传输创建失败: sessionId={}, userId={}", sessionId, fromUser, e);
            throw new RuntimeException("创建传输失败: " + e.getMessage());
        }
    }

    /**
     * 处理传输连接请求
     */
    private void handleTransportConnect(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        MediasoupTransportConnect transportConnect = signaling.getTransportConnect();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在");
            }

            // 解析DTLS参数
            Map<String, Object> dtlsParameters = objectMapper.readValue(
                    transportConnect.getDtlsParameters(), new TypeReference<Map<String, Object>>() {
                    });

            // 连接传输
            mediaRoomService.connectTransport(
                    mediaRoom.getRoomId(),
                    transportConnect.getTransportId(),
                    dtlsParameters
            );

            // 发送连接成功响应
            SignalingMessage successMsg = messageBuilder.buildTransportConnectResponse(
                    sessionId, fromUser, fromDevice,
                    transportConnect.getTransportId(), true, null, requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, successMsg);

            log.info("传输连接成功: sessionId={}, userId={}, transportId={}",
                    sessionId, fromUser, transportConnect.getTransportId());

        } catch (Exception e) {
            log.error("处理传输连接失败: sessionId={}, userId={}, transportId={}",
                    sessionId, fromUser, transportConnect.getTransportId(), e);

            // 发送连接失败响应
            try {
                SignalingMessage errorMsg = messageBuilder.buildTransportConnectResponse(
                        sessionId, fromUser, fromDevice,
                        transportConnect.getTransportId(), false,
                        "传输连接失败: " + e.getMessage(), requestMessageId);

                messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
            } catch (Exception ex) {
                log.error("发送传输连接失败响应异常", ex);
            }

            throw new RuntimeException("连接传输失败: " + e.getMessage());
        }
    }

    /**
     * 处理消费者恢复请求
     */
    private void handleConsumerResume(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        MediasoupConsumerResume consumerResume = signaling.getConsumerResume();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在: sessionId=" + sessionId);
            }

            String peerId = buildPeerId(fromUser, fromDevice);

            // 恢复消费者
            mediaRoomService.resumeConsumer(
                    mediaRoom.getRoomId(),
                    consumerResume.getConsumerId(),
                    peerId
            );

            // 发送恢复成功响应
            SignalingMessage responseMsg = messageBuilder.buildConsumerResumeResponse(
                    sessionId, fromUser, fromDevice,
                    consumerResume.getConsumerId(), true, "消费者恢复成功", requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            log.info("消费者恢复成功: sessionId={}, userId={}, consumerId={}",
                    sessionId, fromUser, consumerResume.getConsumerId());

        } catch (Exception e) {
            log.error("处理消费者恢复失败: sessionId={}, userId={}, consumerId={}",
                    sessionId, fromUser, consumerResume.getConsumerId(), e);

            // 发送恢复失败响应
            try {
                SignalingMessage errorMsg = messageBuilder.buildConsumerResumeResponse(
                        sessionId, fromUser, fromDevice,
                        consumerResume.getConsumerId(), false,
                        "消费者恢复失败: " + e.getMessage(), requestMessageId);

                messageSender.sendToUserDevice(fromUser, fromDevice, errorMsg);
            } catch (Exception ex) {
                log.error("发送消费者恢复失败响应异常", ex);
            }

            throw new RuntimeException("恢复消费者失败: " + e.getMessage());
        }
    }

    /**
     * 通知新生产者方法
     */
    private void notifyNewProducer(Long sessionId, Long fromUser, String fromDevice,
                                   String kind, Map<String, Object> producer) {
        try {
            // 获取会话中其他参与者（排除生产者自己）
            List<Long> otherParticipants = getOtherParticipants(sessionId, fromUser);

            if (!otherParticipants.isEmpty()) {
                ProducerInfo producerInfo = ProducerInfo.newBuilder()
                        .setProducerId((String) producer.get("id"))
                        .setPeerId(buildPeerId(fromUser, fromDevice))
                        .setKind(kind)
                        .build();

                // 构建新生产者通知消息
                SignalingMessage notifyMsg = messageBuilder.buildNewProducerNotifyMessage(
                        sessionId, 0L, "system", otherParticipants, producerInfo);

                // 发送给其他参与者
                messageSender.sendToUsers(otherParticipants, notifyMsg);

                log.info("通知新生产者: sessionId={}, producerId={}, kind={}, notifyUsers={}",
                        sessionId, producer.get("id"), kind, otherParticipants);
            }
        } catch (Exception e) {
            log.error("通知新生产者失败: sessionId={}, userId={}", sessionId, fromUser, e);
        }
    }

    /**
     * 处理生产者创建方法
     */
    private void handleProduce(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        MediasoupProduce produce = signaling.getProduce();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在");
            }

            String peerId = buildPeerId(fromUser, fromDevice);

            // 解析RTP参数
            Map<String, Object> rtpParameters = objectMapper.readValue(
                    produce.getRtpParameters(), new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> producerData = mediaRoomService.produce(
                    mediaRoom.getRoomId(),
                    produce.getTransportId(),
                    produce.getKind(),
                    rtpParameters,
                    peerId
            );

            // 发送生产者创建响应
            SignalingMessage responseMsg = messageBuilder.buildProduceResponse(
                    sessionId, fromUser, fromDevice,
                    (String) producerData.get("id"),
                    produce.getKind(), requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            // 通知房间内其他参与者有新的生产者
            notifyNewProducer(sessionId, fromUser, fromDevice, produce.getKind(), producerData);

            log.info("创建生产者成功: sessionId={}, userId={}, kind={}, producerId={}",
                    sessionId, fromUser, produce.getKind(), producerData.get("id"));

        } catch (Exception e) {
            log.error("处理生产者创建失败: sessionId={}, userId={}", sessionId, fromUser, e);
            throw new RuntimeException("创建生产者失败: " + e.getMessage());
        }
    }


    /**
     * 处理消费者创建方法
     */
    private void handleConsume(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        MediasoupConsume consume = signaling.getConsume();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在");
            }

            String peerId = buildPeerId(fromUser, fromDevice);

            // 解析RTP能力
            Map<String, Object> rtpCapabilities = objectMapper.readValue(
                    consume.getRtpCapabilities(), new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> consumerData = mediaRoomService.consume(
                    mediaRoom.getRoomId(),
                    consume.getTransportId(),
                    consume.getProducerId(),
                    rtpCapabilities,
                    peerId
            );

            // 发送消费者创建响应
            SignalingMessage responseMsg = messageBuilder.buildConsumeResponse(
                    sessionId, fromUser, fromDevice,
                    (String) consumerData.get("id"),
                    consume.getProducerId(),
                    (String) consumerData.get("kind"),
                    objectMapper.writeValueAsString(consumerData.get("rtpParameters")),
                    (String) consumerData.get("type"),
                    requestMessageId
            );

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            log.info("创建消费者成功: sessionId={}, userId={}, producerId={}, consumerId={}",
                    sessionId, fromUser, consume.getProducerId(), consumerData.get("id"));

        } catch (Exception e) {
            log.error("处理消费者创建失败: sessionId={}, userId={}", sessionId, fromUser, e);
            throw new RuntimeException("创建消费者失败: " + e.getMessage());
        }
    }

    /**
     * 处理房间生产者列表请求
     */
    private void handleRoomProducersRequest(SignalingMessage signaling) {
        RoomProducersRequest request = signaling.getRoomProducersRequest();
        Long sessionId = signaling.getSessionId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();
        Long requestMessageId = signaling.getMessageId();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在");
            }

            Map<String, Object> producersData = mediaRoomService.getRoomProducers(
                    mediaRoom.getRoomId());

            // 构建生产者列表响应
            RoomProducersResponse.Builder responseBuilder = RoomProducersResponse.newBuilder()
                    .setRoomId(mediaRoom.getRoomId());

            // 转换生产者数据
            List<Map<String, Object>> producers = (List<Map<String, Object>>) producersData.get("producers");
            for (Map<String, Object> producer : producers) {
                ProducerInfo producerInfo = ProducerInfo.newBuilder()
                        .setProducerId((String) producer.get("producerId"))
                        .setPeerId((String) producer.get("peerId"))
                        .setKind((String) producer.get("kind"))
                        .build();
                responseBuilder.addProducers(producerInfo);
            }

            // 发送响应
            SignalingMessage responseMsg = SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(fromUser)
                    .setType(SignalingType.ROOM_PRODUCERS_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(SignalingMessageBuilder.PROTOCOL_VERSION)
                    .setRoomProducersResponse(responseBuilder.build())
                    .build();

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            log.info("发送房间生产者列表: sessionId={}, userId={}, producerCount={}",
                    sessionId, fromUser, producers.size());

        } catch (Exception e) {
            log.error("处理房间生产者请求失败: sessionId={}, userId={}", sessionId, fromUser, e);
            throw new RuntimeException("获取房间生产者失败: " + e.getMessage());
        }
    }

    /**
     * 处理RTP能力请求
     */
    private void handleRtpCapabilitiesRequest(SignalingMessage signaling) {
        Long requestMessageId = signaling.getMessageId();
        RtpCapabilitiesRequest request = signaling.getRtpCapabilitiesRequest();
        Long sessionId = signaling.getSessionId();
        String roomId = request.getRoomId();
        Long fromUser = signaling.getFromUser();
        String fromDevice = signaling.getFromDevice();

        try {
            MediaRoomMapping mediaRoom = mediaRoomService.getMediaRoomBySessionId(sessionId);
            if (mediaRoom == null) {
                throw new RuntimeException("媒体房间不存在");
            }

            Map<String, Object> rtpCapabilities = mediaRoomService.getRoomRtpCapabilities(
                    mediaRoom.getRoomId());

            String rtpCapabilitiesJson = objectMapper.writeValueAsString(rtpCapabilities);

            SignalingMessage responseMsg = messageBuilder.buildRtpCapabilitiesResponse(
                    sessionId, fromUser, fromDevice, mediaRoom.getRoomId(), rtpCapabilitiesJson, requestMessageId);

            messageSender.sendToUserDevice(fromUser, fromDevice, responseMsg);

            log.info("发送RTP能力响应: sessionId={}, userId={}", sessionId, fromUser);

        } catch (Exception e) {
            log.error("处理RTP能力请求失败: sessionId={}, userId={}", sessionId, fromUser, e);
            throw new RuntimeException("获取RTP能力失败: " + e.getMessage());
        }
    }


    /**
     * 构建peer ID
     */
    private String buildPeerId(Long userId, String deviceId) {
        return userId + "_" + deviceId;
    }

}