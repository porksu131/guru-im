package com.guru.im.offline.model.dto;

// 事件统计模型
public class EventStats {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 事件总数
     */
    private Long totalCount;
    
    /**
     * 最大序列号
     */
    private Long maxSequence;
    
    /**
     * 最早事件时间
     */
    private Long earliestTime;
    
    /**
     * 最新事件时间
     */
    private Long latestTime;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(Long maxSequence) {
        this.maxSequence = maxSequence;
    }

    public Long getEarliestTime() {
        return earliestTime;
    }

    public void setEarliestTime(Long earliestTime) {
        this.earliestTime = earliestTime;
    }

    public Long getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(Long latestTime) {
        this.latestTime = latestTime;
    }
}