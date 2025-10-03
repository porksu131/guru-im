package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.SyncCursor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SyncCursorMapper {

    int insert(SyncCursor syncCursor);

    int update(SyncCursor syncCursor);

    SyncCursor findByUserDeviceConversation(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("conversationType") Integer conversationType,
            @Param("conversationId") Long conversationId);

    List<SyncCursor> findByUserAndDevice(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId);

    int updateLastSyncSeq(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("conversationType") Integer conversationType,
            @Param("conversationId") Long conversationId,
            @Param("lastSyncSeq") Long lastSyncSeq,
            @Param("updateTime") Long updateTime);

    void updateSyncStatus(
            @Param("syncId") Long syncId,
            @Param("status") Integer status,
            @Param("message") String message,
            @Param("updateTime") Long updateTime);

    Map<String, Object> findSyncStatus(@Param("syncId") Long syncId);

    Integer countNewMessages(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId,
            @Param("lastSyncSeq") Long lastSyncSeq);
}