package com.guru.im.offline.model.dto;

// 用户序列号模型
public class UserSequence {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 最大序列号
     */
    private Long maxSequence;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(Long maxSequence) {
        this.maxSequence = maxSequence;
    }
}