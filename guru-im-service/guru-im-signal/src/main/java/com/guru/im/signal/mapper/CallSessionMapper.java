package com.guru.im.signal.mapper;

import com.guru.im.signal.model.pojo.CallSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CallSessionMapper {

    int insert(CallSession callSession);

    CallSession selectBySessionId(@Param("sessionId") Long sessionId);

    int updateCallState(@Param("sessionId") Long sessionId,
                        @Param("callState") Integer callState,
                        @Param("updateTime") Long updateTime);

    List<CallSession> selectUserActiveSessions(@Param("userId") Long userId);

    int updateSessionEndInfo(@Param("sessionId") Long sessionId,
                             @Param("endTime") Long endTime,
                             @Param("duration") Integer duration,
                             @Param("hangupReason") String hangupReason,
                             @Param("hangupType") Integer hangupType,
                             @Param("hangupInitiator") Long hangupInitiator,
                             @Param("updateTime") Long updateTime);

    int updateStartTime(@Param("sessionId") Long sessionId,
                        @Param("startTime") Long startTime,
                        @Param("updateTime") Long updateTime);

    int update(CallSession callSession);

    List<CallSession> selectActiveSessions();

    List<CallSession> selectDialingSessions();
}