package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.SyncSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SyncSessionMapper {

    /**
     * 插入同步会话
     */
    int insert(SyncSession syncSession);

    /**
     * 根据ID查询同步会话
     */
    SyncSession selectById(@Param("id") Long id);

    /**
     * 根据同步ID查询会话
     */
    SyncSession findBySyncId(@Param("syncId") Long syncId);

    /**
     * 根据用户和设备查询活跃会话
     */
    List<SyncSession> findActiveSessionsByUserAndDevice(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId);

    /**
     * 更新同步状态
     */
    int updateSyncStatus(
            @Param("syncId") Long syncId,
            @Param("syncStatus") Integer syncStatus,
            @Param("errorMessage") String errorMessage,
            @Param("updateTime") Long updateTime);

    /**
     * 更新同步进度
     */
    int updateSyncProgress(
            @Param("syncId") Long syncId,
            @Param("syncedCount") Integer syncedCount,
            @Param("currentBatch") Integer currentBatch,
            @Param("lastSyncedSeq") Long lastSyncedSeq,
            @Param("lastActivityTime") Long lastActivityTime,
            @Param("updateTime") Long updateTime);

    /**
     * 完成同步会话
     */
    int completeSyncSession(
            @Param("syncId") Long syncId,
            @Param("syncStatus") Integer syncStatus,
            @Param("endTime") Long endTime,
            @Param("updateTime") Long updateTime);

    /**
     * 查询过期的同步会话
     */
    List<SyncSession> findExpiredSessions(@Param("expireTime") Long expireTime);

    /**
     * 批量更新会话状态
     */
    int batchUpdateStatus(
            @Param("syncIds") List<Long> syncIds,
            @Param("syncStatus") Integer syncStatus,
            @Param("updateTime") Long updateTime);

    /**
     * 统计用户的同步会话数量
     */
    Integer countByUserAndStatus(
            @Param("userId") Long userId,
            @Param("syncStatus") Integer syncStatus);

    /**
     * 查询同步会话统计信息
     */
    Map<String, Object> findSyncStats(@Param("syncId") Long syncId);

}