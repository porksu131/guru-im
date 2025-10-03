package com.guru.im.offline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "im.sync.full")
public class FullSyncConfig {
    
    /**
     * 全量同步配置
     */
    private int maxMessages = 1000;          // 最大同步消息数
    private int batchSize = 50;              // 批次大小
    private int timeoutSeconds = 60;         // 超时时间
    private int maxBatches = 100;            // 最大批次数量
    
    /**
     * 会话同步策略
     */
    private int smallUnreadThreshold = 99;   // 小未读数阈值
    private int smallUnreadBonus = 20;       // 小未读数额外同步数量
    private int largeUnreadLimit = 20;       // 大未读数同步限制
    private int maxPerConversation = 100;    // 单会话最大同步数量


    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxBatches() {
        return maxBatches;
    }

    public void setMaxBatches(int maxBatches) {
        this.maxBatches = maxBatches;
    }

    public int getSmallUnreadThreshold() {
        return smallUnreadThreshold;
    }

    public void setSmallUnreadThreshold(int smallUnreadThreshold) {
        this.smallUnreadThreshold = smallUnreadThreshold;
    }

    public int getSmallUnreadBonus() {
        return smallUnreadBonus;
    }

    public void setSmallUnreadBonus(int smallUnreadBonus) {
        this.smallUnreadBonus = smallUnreadBonus;
    }

    public int getLargeUnreadLimit() {
        return largeUnreadLimit;
    }

    public void setLargeUnreadLimit(int largeUnreadLimit) {
        this.largeUnreadLimit = largeUnreadLimit;
    }

    public int getMaxPerConversation() {
        return maxPerConversation;
    }

    public void setMaxPerConversation(int maxPerConversation) {
        this.maxPerConversation = maxPerConversation;
    }
}