package com.guru.im.offline.model.pojo;

public class SyncSession {
    /**
     * 会话ID(雪花ID)
     */
    private Long id;
    
    /**
     * 同步会话ID
     */
    private Long syncId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 同步类型(0:全量 1:增量 2:按会话 3:按优先级)
     */
    private Integer syncType;
    
    /**
     * 同步策略
     */
    private String syncStrategy;
    
    /**
     * 总消息数量
     */
    private Integer totalCount;
    
    /**
     * 已同步数量
     */
    private Integer syncedCount;
    
    /**
     * 当前批次
     */
    private Integer currentBatch;
    
    /**
     * 批次大小
     */
    private Integer batchSize;
    
    /**
     * 同步状态(1:进行中 2:已推送MQ 3:已送达 4:推送MQ失败 5:送达失败)
     */
    private Integer syncStatus;
    
    /**
     * 开始时间
     */
    private Long startTime;
    
    /**
     * 结束时间
     */
    private Long endTime;
    
    /**
     * 最后活动时间
     */
    private Long lastActivityTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 同步起始消息序列号
     */
    private Long syncStartSeq;
    
    /**
     * 同步结束消息序列号(计划)
     */
    private Long syncEndSeq;
    
    /**
     * 最后已同步的消息序列号(用于断点续传)
     */
    private Long lastSyncedSeq;
    
    /**
     * 创建时间(时间戳)
     */
    private Long createTime;
    
    /**
     * 更新时间(时间戳)
     */
    private Long updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSyncId() {
        return syncId;
    }

    public void setSyncId(Long syncId) {
        this.syncId = syncId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getSyncType() {
        return syncType;
    }

    public void setSyncType(Integer syncType) {
        this.syncType = syncType;
    }

    public String getSyncStrategy() {
        return syncStrategy;
    }

    public void setSyncStrategy(String syncStrategy) {
        this.syncStrategy = syncStrategy;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(Integer syncedCount) {
        this.syncedCount = syncedCount;
    }

    public Integer getCurrentBatch() {
        return currentBatch;
    }

    public void setCurrentBatch(Integer currentBatch) {
        this.currentBatch = currentBatch;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(Integer syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(Long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getSyncStartSeq() {
        return syncStartSeq;
    }

    public void setSyncStartSeq(Long syncStartSeq) {
        this.syncStartSeq = syncStartSeq;
    }

    public Long getSyncEndSeq() {
        return syncEndSeq;
    }

    public void setSyncEndSeq(Long syncEndSeq) {
        this.syncEndSeq = syncEndSeq;
    }

    public Long getLastSyncedSeq() {
        return lastSyncedSeq;
    }

    public void setLastSyncedSeq(Long lastSyncedSeq) {
        this.lastSyncedSeq = lastSyncedSeq;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}