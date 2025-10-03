package com.guru.im.offline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "guru.im.offline.sync")
public class SyncConfig {
    private Integer defaultBatchSize = 100;
    private Long sessionExpireHours = 24L;
    private Integer maxRetryCount = 3;
    private Boolean enableCompression = true;


    public Integer getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public void setDefaultBatchSize(Integer defaultBatchSize) {
        this.defaultBatchSize = defaultBatchSize;
    }

    public Long getSessionExpireHours() {
        return sessionExpireHours;
    }

    public void setSessionExpireHours(Long sessionExpireHours) {
        this.sessionExpireHours = sessionExpireHours;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Boolean getEnableCompression() {
        return enableCompression;
    }

    public void setEnableCompression(Boolean enableCompression) {
        this.enableCompression = enableCompression;
    }
}