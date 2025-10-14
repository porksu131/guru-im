package com.guru.im.signal.service;

import com.guru.im.protocol.model.CallRequest;
import com.guru.im.protocol.model.ConferenceInvite;
import com.guru.im.signal.model.pojo.CallParticipant;
import com.guru.im.signal.model.pojo.CallSession;

import java.util.List;

public interface CallSessionService {
    
    CallSession createCallSession(Long sessionId, Long initiatorId, String initiatorDevice, CallRequest callRequest);
    
    CallSession getCallSession(Long sessionId);
    
    void updateCallState(Long sessionId, CallSession.CallState callState);
    
    boolean allParticipantsAccepted(Long sessionId);
    
    void endCallSession(Long sessionId, String reason, Integer hangupType, Long initiatorId);
    
    List<CallParticipant> getSessionParticipants(Long sessionId);
    
    List<Long> getParticipantUserIds(Long sessionId);
    
    void addParticipant(Long sessionId, Long userId, String deviceId, boolean isInviter);
    
    void updateParticipantState(Long sessionId, Long userId, String deviceId, CallParticipant.ParticipantState state);
    
    List<CallSession> getUserActiveSessions(Long userId);

    CallSession createConferenceSession(Long initiatorId, String initiatorDevice, ConferenceInvite conferenceInvite);

    CallParticipant getParticipant(Long sessionId, Long userId, String deviceId);

    List<CallSession> getActiveSessions();

    List<CallSession> getDialingSessions();

    void updateSessionDuration(Long sessionId);
}