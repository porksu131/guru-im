package com.guru.im.signal.mapper;

import com.guru.im.signal.model.pojo.CallParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CallParticipantMapper {

    int insert(CallParticipant callParticipant);

    List<CallParticipant> selectSessionParticipants(@Param("sessionId") Long sessionId);

    int updateParticipantState(@Param("sessionId") Long sessionId,
                               @Param("userId") Long userId,
                               @Param("deviceId") String deviceId,
                               @Param("state") Integer state,
                               @Param("updateTime") Long updateTime);

    int updateParticipantJoinInfo(@Param("sessionId") Long sessionId,
                                  @Param("userId") Long userId,
                                  @Param("deviceId") String deviceId,
                                  @Param("joinTime") Long joinTime,
                                  @Param("updateTime") Long updateTime);

    int updateParticipantLeaveInfo(@Param("sessionId") Long sessionId,
                                   @Param("userId") Long userId,
                                   @Param("deviceId") String deviceId,
                                   @Param("leaveTime") Long leaveTime,
                                   @Param("duration") Integer duration,
                                   @Param("updateTime") Long updateTime);

    int countParticipantsByState(@Param("sessionId") Long sessionId,
                                 @Param("states") List<Integer> states);

    CallParticipant selectParticipant(@Param("sessionId") Long sessionId,
                                      @Param("userId") Long userId,
                                      @Param("deviceId") String deviceId);

    int deleteBySessionId(@Param("sessionId") Long sessionId);
}