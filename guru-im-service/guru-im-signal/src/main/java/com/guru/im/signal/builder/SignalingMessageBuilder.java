package com.guru.im.signal.builder;

import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.protocol.model.*;
import com.guru.im.signal.model.pojo.CallSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SignalingMessageBuilder {
    public static final int PROTOCOL_VERSION = 1;
    private static final Logger log = LoggerFactory.getLogger(SignalingMessageBuilder.class);
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 构建呼叫邀请消息
     */
    public SignalingMessage buildCallInviteMessage(CallSession session, List<Long> toUsers, CallRequest callRequest) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(session.getId())
                    .setFromUser(session.getInitiatorId())
                    .setFromDevice(session.getInitiatorDevice())
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.CALL_REQUEST)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallRequest(callRequest != null ? callRequest : buildDefaultCallRequest(session))
                    .build();
        } catch (Exception e) {
            log.error("构建呼叫邀请消息失败: sessionId={}", session.getId(), e);
            throw new RuntimeException("构建呼叫邀请消息失败");
        }
    }

    /**
     * 构建呼叫请求响应消息
     */
    public SignalingMessage buildCallRequestResponse(Long sessionId, Long toUser, String toDevice,
                                                     String roomId, boolean success, String errorMessage,
                                                     Long requestMessageId) {
        try {
            CallRequestResponse response = CallRequestResponse.newBuilder()
                    .setSessionId(sessionId)
                    .setRoomId(roomId)
                    .setSuccess(success)
                    .setErrorMessage(errorMessage != null ? errorMessage : "")
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.CALL_REQUEST_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallRequestResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("构建呼叫请求响应消息失败", e);
            throw new RuntimeException("构建呼叫请求响应消息失败");
        }
    }

    /**
     * 构建呼叫接受消息
     */
    public SignalingMessage buildCallAcceptMessage(Long sessionId, Long userId, String deviceId,
                                                   CallResponse callResponse) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CALL_ACCEPT)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallResponse(callResponse != null ? callResponse :
                            CallResponse.newBuilder()
                                    .setAccepted(true)
                                    .build())
                    .build();
        } catch (Exception e) {
            log.error("构建呼叫接受消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建呼叫接受消息失败");
        }
    }

    /**
     * 构建通话开始消息
     */
    public SignalingMessage buildCallStartedMessage(Long sessionId, List<Long> toUsers,
                                                    List<Participant> participants) {
        try {
            CallStateSync stateSync = CallStateSync.newBuilder()
                    .setSessionId(sessionId)
                    .setCallState(CallStateSync.CallState.ACTIVE)
                    .addAllParticipants(participants != null ? participants : Collections.emptyList())
                    .setStateTimestamp(System.currentTimeMillis())
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L) // 系统用户
                    .setFromDevice("system")
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.STATE_SYNC)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setStateSync(stateSync)
                    .build();
        } catch (Exception e) {
            log.error("构建通话开始消息失败: sessionId={}", sessionId, e);
            throw new RuntimeException("构建通话开始消息失败");
        }
    }

    /**
     * 构建通话结束消息
     */
    public SignalingMessage buildCallEndedMessage(Long sessionId, List<Long> toUsers,
                                                  String reason, Integer duration) {
        try {
            CallStateSync stateSync = CallStateSync.newBuilder()
                    .setSessionId(sessionId)
                    .setCallState(CallStateSync.CallState.ENDED)
                    .setStateTimestamp(System.currentTimeMillis())
                    .build();

            SignalingMessage.Builder builder = SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.STATE_SYNC)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setStateSync(stateSync);

            return builder.build();
        } catch (Exception e) {
            log.error("构建通话结束消息失败: sessionId={}", sessionId, e);
            throw new RuntimeException("构建通话结束消息失败");
        }
    }

    /**
     * 构建通话状态同步消息
     */
    public SignalingMessage buildCallStateSyncMessage(Long sessionId, List<Long> toUsers,
                                                      CallStateSync.CallState callState,
                                                      List<Participant> participants) {
        try {
            CallStateSync stateSync = CallStateSync.newBuilder()
                    .setSessionId(sessionId)
                    .setCallState(callState)
                    .addAllParticipants(participants != null ? participants : Collections.emptyList())
                    .setStateTimestamp(System.currentTimeMillis())
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.STATE_SYNC)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setStateSync(stateSync)
                    .build();
        } catch (Exception e) {
            log.error("构建通话状态同步消息失败: sessionId={}, state={}", sessionId, callState, e);
            throw new RuntimeException("构建通话状态同步消息失败");
        }
    }

    /**
     * 构建参与者状态更新消息
     */
    public SignalingMessage buildParticipantUpdateMessage(Long sessionId, List<Long> toUsers,
                                                          Participant participant) {
        try {
            CallStateSync stateSync = CallStateSync.newBuilder()
                    .setSessionId(sessionId)
                    .setCallState(CallStateSync.CallState.ACTIVE)
                    .addParticipants(participant)
                    .setStateTimestamp(System.currentTimeMillis())
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.STATE_SYNC)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setStateSync(stateSync)
                    .build();
        } catch (Exception e) {
            log.error("构建参与者状态更新消息失败: sessionId={}, userId={}",
                    sessionId, participant.getUserId(), e);
            throw new RuntimeException("构建参与者状态更新消息失败");
        }
    }

    /**
     * 构建呼叫拒绝消息
     */
    public SignalingMessage buildCallRejectMessage(Long sessionId, Long userId, String deviceId, String reason) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CALL_REJECT)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason(reason != null ? reason : "用户拒绝通话")
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("构建呼叫拒绝消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建呼叫拒绝消息失败");
        }
    }

    /**
     * 构建呼叫取消消息
     */
    public SignalingMessage buildCallCancelMessage(Long sessionId, Long userId, String deviceId, String reason) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CALL_CANCEL)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason(reason != null ? reason : "用户取消通话")
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("构建呼叫取消消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建呼叫取消消息失败");
        }
    }

    /**
     * 构建通话挂断消息
     */
    public SignalingMessage buildCallHangupMessage(Long sessionId, Long userId, String deviceId,
                                                   CallHangup hangup) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CALL_HANGUP)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setHangup(hangup != null ? hangup : buildDefaultHangup(userId))
                    .build();
        } catch (Exception e) {
            log.error("构建通话挂断消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建通话挂断消息失败");
        }
    }

    /**
     * 构建振铃消息
     */
    public SignalingMessage buildRingingMessage(Long sessionId, Long userId, String deviceId,
                                                Ringing ringing) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.RINGING)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setRinging(ringing)
                    .build();
        } catch (Exception e) {
            log.error("构建振铃消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建振铃消息失败");
        }
    }

    /**
     * 构建状态同步消息
     */
    public SignalingMessage buildStateSyncMessage(Long sessionId, CallStateSync stateSync) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(0L) // 系统用户
                    .setFromDevice("system")
                    .setType(SignalingType.STATE_SYNC)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setStateSync(stateSync)
                    .build();
        } catch (Exception e) {
            log.error("构建状态同步消息失败: sessionId={}", sessionId, e);
            throw new RuntimeException("构建状态同步消息失败");
        }
    }

    /**
     * 构建会议邀请消息
     */
    public SignalingMessage buildConferenceInviteMessage(CallSession session, Long invitee,
                                                         ConferenceInvite conferenceInvite) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(session.getId())
                    .setFromUser(session.getInitiatorId())
                    .setFromDevice(session.getInitiatorDevice())
                    .addToUsers(invitee)
                    .setType(SignalingType.CONFERENCE_INVITE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConferenceInvite(conferenceInvite)
                    .build();
        } catch (Exception e) {
            log.error("构建会议邀请消息失败: sessionId={}, invitee={}", session.getId(), invitee, e);
            throw new RuntimeException("构建会议邀请消息失败");
        }
    }

    /**
     * 构建会议邀请响应消息
     */
    public SignalingMessage buildConferenceInviteResponse(Long sessionId, Long toUser, String toDevice,
                                                          String roomId, boolean success, String errorMessage,
                                                          Long requestMessageId) {
        try {
            ConferenceInviteResponse response = ConferenceInviteResponse.newBuilder()
                    .setSessionId(sessionId)
                    .setRoomId(roomId)
                    .setSuccess(success)
                    .setErrorMessage(errorMessage != null ? errorMessage : "")
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.CONFERENCE_INVITE_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConferenceInviteResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("构建会议邀请响应消息失败", e);
            throw new RuntimeException("构建会议邀请响应消息失败");
        }
    }

    /**
     * 构建会议加入消息
     */
    public SignalingMessage buildConferenceJoinMessage(Long sessionId, Long userId, String deviceId,
                                                       ConferenceJoin conferenceJoin) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CONFERENCE_JOIN)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConferenceJoin(conferenceJoin != null ? conferenceJoin :
                            ConferenceJoin.newBuilder().build())
                    .build();
        } catch (Exception e) {
            log.error("构建会议加入消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建会议加入消息失败");
        }
    }

    /**
     * 构建会议离开消息
     */
    public SignalingMessage buildConferenceLeaveMessage(Long sessionId, Long userId, String deviceId,
                                                        ConferenceLeave conferenceLeave) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(userId)
                    .setFromDevice(deviceId)
                    .setType(SignalingType.CONFERENCE_LEAVE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConferenceLeave(conferenceLeave)
                    .build();
        } catch (Exception e) {
            log.error("构建会议离开消息失败: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("构建会议离开消息失败");
        }
    }

    /**
     * 构建媒体控制消息
     */
    public SignalingMessage buildMediaControlMessage(Long sessionId, Long fromUser, String fromDevice,
                                                     List<Long> toUsers, MediaControl mediaControl) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(fromUser)
                    .setFromDevice(fromDevice)
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.MEDIA_CONTROL)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setMediaControl(mediaControl)
                    .build();
        } catch (Exception e) {
            log.error("构建媒体控制消息失败: sessionId={}, fromUser={}", sessionId, fromUser, e);
            throw new RuntimeException("构建媒体控制消息失败");
        }
    }

    /**
     * 构建错误响应消息
     */
    public SignalingMessage buildErrorResponseMessage(Long sessionId, Long toUser, String deviceId,
                                                      String errorMessage, Long requestMessageId) {
        try {
            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L) // 系统用户
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.CALL_REJECT)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason(errorMessage)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("构建错误响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建错误响应消息失败");
        }
    }

    /**
     * 构建默认的呼叫请求
     */
    private CallRequest buildDefaultCallRequest(CallSession session) {
        return CallRequest.newBuilder()
                .setMediaType(MediaType.forNumber(session.getMediaType()))
                .setTimeoutSeconds(session.getTimeoutSeconds() != null ? session.getTimeoutSeconds() : 30)
                .setCallSubject(session.getCallSubject() != null ? session.getCallSubject() : "通话邀请")
                .build();
    }

    /**
     * 构建默认的挂断信息
     */
    private CallHangup buildDefaultHangup(Long userId) {
        return CallHangup.newBuilder()
                .setReason("用户主动挂断")
                .setHangupType(CallHangup.HangupType.USER_HANGUP)
                .setInitiatedBy(userId)
                .build();
    }


    /**
     * 构建消费者恢复响应消息
     */
    public SignalingMessage buildConsumerResumeResponse(Long sessionId, Long toUser, String toDevice,
                                                        String consumerId, boolean success, String message,
                                                        Long requestMessageId) {
        try {
            MediasoupConsumerResume response = MediasoupConsumerResume.newBuilder()
                    .setConsumerId(consumerId)
                    .build();

            SignalingMessage.Builder builder = SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L) // 系统用户
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.MEDIASOUP_CONSUMER_RESUME)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConsumerResume(response);

            // 添加额外信息到extra fields（如果需要）
            if (!success) {
                // 可以在这里添加错误信息
            }

            return builder.build();
        } catch (Exception e) {
            log.error("构建消费者恢复响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建消费者恢复响应消息失败");
        }
    }

    /**
     * 构建新生产者通知消息
     */
    public SignalingMessage buildNewProducerNotifyMessage(Long sessionId, Long fromUser, String fromDevice,
                                                          List<Long> toUsers, ProducerInfo producerInfo) {
        try {
            // 使用RoomProducersResponse来通知新生产者
            RoomProducersResponse response = RoomProducersResponse.newBuilder()
                    .setRoomId("room_" + sessionId) // 根据sessionId构建roomId
                    .addProducers(producerInfo)
                    .setIsNewProducer(true) // 标记为新生产者通知
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(snowflakeIdGenerator.nextId())
                    .setSessionId(sessionId)
                    .setFromUser(fromUser)
                    .setFromDevice(fromDevice)
                    .addAllToUsers(toUsers)
                    .setType(SignalingType.ROOM_PRODUCERS_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setRoomProducersResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("构建新生产者通知消息失败: sessionId={}, fromUser={}", sessionId, fromUser, e);
            throw new RuntimeException("构建新生产者通知消息失败");
        }
    }


    /**
     * 构建mediasoup传输创建响应消息
     */
    public SignalingMessage buildTransportCreateResponse(Long sessionId, Long toUser, String toDevice,
                                                         String roomId, String transportOptionsJson,
                                                         Long requestMessageId) {
        try {
            MediasoupTransportCreate response = MediasoupTransportCreate.newBuilder()
                    .setRoomId(roomId)
                    .setTransportOptions(transportOptionsJson)
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.MEDIASOUP_TRANSPORT_CREATE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setTransportCreate(response)
                    .build();
        } catch (Exception e) {
            log.error("构建传输创建响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建传输创建响应消息失败");
        }
    }

    /**
     * 构建mediasoup传输连接响应消息
     */
    public SignalingMessage buildTransportConnectResponse(Long sessionId, Long toUser, String toDevice,
                                                          String transportId, boolean success, String errorMessage,
                                                          Long requestMessageId) {
        try {
            MediasoupTransportConnectResponse response = MediasoupTransportConnectResponse.newBuilder()
                    .setTransportId(transportId)
                    .setSuccess(success)
                    .setErrorMessage(errorMessage != null ? errorMessage : "")
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.MEDIASOUP_TRANSPORT_CONNECT_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setTransportConnectResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("构建传输连接响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建传输连接响应消息失败");
        }
    }

    /**
     * 构建mediasoup生产者创建响应消息
     */
    public SignalingMessage buildProduceResponse(Long sessionId, Long toUser, String toDevice,
                                                 String producerId, String kind, Long requestMessageId) {
        try {
            MediasoupProduceResponse produceResponse = MediasoupProduceResponse.newBuilder()
                    .setProducerId(producerId)
                    .setKind(kind)
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.MEDIASOUP_PRODUCE_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setProduceResponse(produceResponse)
                    .build();
        } catch (Exception e) {
            log.error("构建生产者创建响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建生产者创建响应消息失败");
        }
    }

    /**
     * 构建mediasoup消费者创建响应消息
     */
    public SignalingMessage buildConsumeResponse(Long sessionId, Long toUser, String toDevice,
                                                 String consumerId, String producerId, String kind,
                                                 String rtpParametersJson, String type, Long requestMessageId) {
        try {
            MediasoupConsumeResponse consumeResponse = MediasoupConsumeResponse.newBuilder()
                    .setConsumerId(consumerId)
                    .setProducerId(producerId)
                    .setKind(kind)
                    .setRtpParameters(rtpParametersJson)
                    .setType(type)
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.MEDIASOUP_CONSUME_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setConsumeResponse(consumeResponse)
                    .build();
        } catch (Exception e) {
            log.error("构建消费者创建响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建消费者创建响应消息失败");
        }
    }

    /**
     * 构建RTP能力响应消息
     */
    public SignalingMessage buildRtpCapabilitiesResponse(Long sessionId, Long toUser, String toDevice,
                                                         String roomId, String rtpCapabilitiesJson, Long requestMessageId) {
        try {
            RtpCapabilitiesResponse response = RtpCapabilitiesResponse.newBuilder()
                    .setRoomId(roomId)
                    .setRtpCapabilities(rtpCapabilitiesJson)
                    .build();

            return SignalingMessage.newBuilder()
                    .setMessageId(requestMessageId)
                    .setSessionId(sessionId)
                    .setFromUser(0L)
                    .setFromDevice("system")
                    .addToUsers(toUser)
                    .setType(SignalingType.RTP_CAPABILITIES_RESPONSE)
                    .setTimestamp(System.currentTimeMillis())
                    .setVersion(PROTOCOL_VERSION)
                    .setRtpCapabilitiesResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("构建RTP能力响应消息失败: sessionId={}, toUser={}", sessionId, toUser, e);
            throw new RuntimeException("构建RTP能力响应消息失败");
        }
    }

}