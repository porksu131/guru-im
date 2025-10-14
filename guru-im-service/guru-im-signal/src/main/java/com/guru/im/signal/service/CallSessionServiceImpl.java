package com.guru.im.signal.service;

import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.protocol.model.CallRequest;
import com.guru.im.protocol.model.ConferenceInvite;
import com.guru.im.signal.mapper.CallParticipantMapper;
import com.guru.im.signal.mapper.CallSessionMapper;
import com.guru.im.signal.model.pojo.CallParticipant;
import com.guru.im.signal.model.pojo.CallSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CallSessionServiceImpl implements CallSessionService {
    private static final Logger log = LoggerFactory.getLogger(CallSessionServiceImpl.class);
    private static int MAX_PARTICIPANTS = 30;
    @Autowired
    private CallSessionMapper callSessionMapper;
    
    @Autowired
    private CallParticipantMapper callParticipantMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional
    public CallSession createCallSession(Long sessionId, Long initiatorId, String initiatorDevice, CallRequest callRequest) {
        long now = System.currentTimeMillis();
        
        // 创建通话会话
        CallSession session = new CallSession();
        session.setId(sessionId);
        session.setSessionType(callRequest.getGroupId() > 0 ? 
            CallSession.SessionType.GROUP.getValue() : CallSession.SessionType.PRIVATE.getValue());
        session.setMediaType(callRequest.getMediaType().getNumber());
        session.setInitiatorId(initiatorId);
        session.setInitiatorDevice(initiatorDevice);
        session.setGroupId(callRequest.getGroupId());
        session.setCallSubject(callRequest.getCallSubject());
        session.setCallState(CallSession.CallState.IDLE.getValue());
        session.setMaxParticipants(MAX_PARTICIPANTS);
        session.setTimeoutSeconds(callRequest.getTimeoutSeconds());
        session.setCreateTime(now);
        session.setUpdateTime(now);
        
        callSessionMapper.insert(session);
        
        // 添加发起者作为参与者
        addParticipant(sessionId, initiatorId, initiatorDevice, true);
        
        log.info("创建通话会话成功: sessionId={}, initiator={}", sessionId, initiatorId);
        return session;
    }

    @Override
    public CallSession getCallSession(Long sessionId) {
        return callSessionMapper.selectBySessionId(sessionId);
    }

    @Override
    public void updateCallState(Long sessionId, CallSession.CallState callState) {
        long now = System.currentTimeMillis();
        callSessionMapper.updateCallState(sessionId, callState.getValue(), now);
        
        // 如果是通话开始状态，记录开始时间
        if (callState == CallSession.CallState.ACTIVE) {
            CallSession session = getCallSession(sessionId);
            if (session != null && session.getStartTime() == null) {
                session.setStartTime(now);
                session.setUpdateTime(now);
                callSessionMapper.updateStartTime(sessionId, now, now);
            }
        }
    }

    @Override
    public boolean allParticipantsAccepted(Long sessionId) {
        // 统计已加入的参与者数量
        Integer joinedCount = callParticipantMapper.countParticipantsByState(sessionId, 
            Arrays.asList(CallParticipant.ParticipantState.JOINED.getValue()));
        
        // 统计总参与者数量（排除已拒绝和超时的）
        Integer totalCount = callParticipantMapper.countParticipantsByState(sessionId,
            Arrays.asList(
                CallParticipant.ParticipantState.INVITED.getValue(),
                CallParticipant.ParticipantState.RINGING.getValue(), 
                CallParticipant.ParticipantState.JOINED.getValue(),
                CallParticipant.ParticipantState.RECONNECTING.getValue()
            ));
        
        return joinedCount > 0 && joinedCount.equals(totalCount);
    }

    @Override
    @Transactional
    public void endCallSession(Long sessionId, String reason, Integer hangupType, Long initiatorId) {
        long now = System.currentTimeMillis();
        CallSession session = getCallSession(sessionId);
        if (session == null) {
            log.warn("通话会话不存在: sessionId={}", sessionId);
            return;
        }
        
        // 计算通话时长
        Integer duration = 0;
        if (session.getStartTime() != null) {
            duration = (int) ((now - session.getStartTime()) / 1000);
        }
        
        // 更新会话结束信息
        callSessionMapper.updateSessionEndInfo(sessionId, now, duration, reason, hangupType, initiatorId, now);
        
        // 更新所有参与者的离开状态
        List<CallParticipant> participants = getSessionParticipants(sessionId);
        for (CallParticipant participant : participants) {
            if (participant.getParticipantState() == CallParticipant.ParticipantState.JOINED.getValue() ||
                participant.getParticipantState() == CallParticipant.ParticipantState.RECONNECTING.getValue()) {
                
                callParticipantMapper.updateParticipantLeaveInfo(
                    sessionId, participant.getUserId(), participant.getDeviceId(),
                    now, duration, now
                );
            }
        }
        
        log.info("结束通话会话: sessionId={}, reason={}, duration={}s", sessionId, reason, duration);
    }

    @Override
    public List<CallParticipant> getSessionParticipants(Long sessionId) {
        return callParticipantMapper.selectSessionParticipants(sessionId);
    }

    @Override
    public List<Long> getParticipantUserIds(Long sessionId) {
        return getSessionParticipants(sessionId).stream()
            .map(CallParticipant::getUserId)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addParticipant(Long sessionId, Long userId, String deviceId, boolean isInviter) {
        long now = System.currentTimeMillis();
        
        CallParticipant participant = new CallParticipant();
        participant.setId(snowflakeIdGenerator.nextId());
        participant.setSessionId(sessionId);
        participant.setUserId(userId);
        participant.setDeviceId(deviceId);
        participant.setParticipantState(CallParticipant.ParticipantState.INVITED.getValue());
        participant.setIsInviter(isInviter ? 1 : 0);
        participant.setCreateTime(now);
        participant.setUpdateTime(now);
        
        callParticipantMapper.insert(participant);
        
        log.info("添加通话参与者: sessionId={}, userId={}, deviceId={}", sessionId, userId, deviceId);
    }

    @Override
    public void updateParticipantState(Long sessionId, Long userId, String deviceId, CallParticipant.ParticipantState state) {
        long now = System.currentTimeMillis();
        
        if (state == CallParticipant.ParticipantState.JOINED) {
            // 加入时记录加入时间
            callParticipantMapper.updateParticipantJoinInfo(sessionId, userId, deviceId, now, now);
        } else {
            callParticipantMapper.updateParticipantState(sessionId, userId, deviceId, state.getValue(), now);
        }
        
        log.info("更新参与者状态: sessionId={}, userId={}, state={}", sessionId, userId, state);
    }

    @Override
    public List<CallSession> getUserActiveSessions(Long userId) {
        return callSessionMapper.selectUserActiveSessions(userId);
    }

    @Override
    @Transactional
    public CallSession createConferenceSession(Long initiatorId, String initiatorDevice, ConferenceInvite conferenceInvite) {
        long now = System.currentTimeMillis();
        Long sessionId = snowflakeIdGenerator.nextId();

        CallSession session = new CallSession();
        session.setId(sessionId);
        session.setSessionType(CallSession.SessionType.CONFERENCE.getValue());
        session.setMediaType(conferenceInvite.getMediaType().getNumber());
        session.setInitiatorId(initiatorId);
        session.setInitiatorDevice(initiatorDevice);
        session.setConferenceId(conferenceInvite.getConferenceId());
        session.setConferenceTitle(conferenceInvite.getConferenceTitle());
        session.setCallSubject(conferenceInvite.getConferenceTitle());
        session.setCallState(CallSession.CallState.DIALING.getValue());
        session.setMaxParticipants(50); // 会议默认支持更多人
        session.setTimeoutSeconds(3600); // 会议超时时间更长
        session.setCreateTime(now);
        session.setUpdateTime(now);

        callSessionMapper.insert(session);

        // 添加发起者作为参与者
        addParticipant(sessionId, initiatorId, initiatorDevice, true);

        log.info("创建会议会话成功: sessionId={}, conferenceId={}, host={}",
                sessionId, conferenceInvite.getConferenceId(), initiatorId);
        return session;
    }

    @Override
    public CallParticipant getParticipant(Long sessionId, Long userId, String deviceId) {
        return callParticipantMapper.selectParticipant(sessionId, userId, deviceId);
    }

    @Override
    public List<CallSession> getActiveSessions() {
        return callSessionMapper.selectActiveSessions();
    }

    @Override
    public List<CallSession> getDialingSessions() {
        return callSessionMapper.selectDialingSessions();
    }

    @Override
    @Transactional
    public void updateSessionDuration(Long sessionId) {
        CallSession session = getCallSession(sessionId);
        if (session == null || session.getStartTime() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        int duration = (int) ((now - session.getStartTime()) / 1000);

        // 只更新时长和更新时间
        session.setDuration(duration);
        session.setUpdateTime(now);

        callSessionMapper.update(session);

        log.debug("更新通话时长: sessionId={}, duration={}s", sessionId, duration);
    }
}