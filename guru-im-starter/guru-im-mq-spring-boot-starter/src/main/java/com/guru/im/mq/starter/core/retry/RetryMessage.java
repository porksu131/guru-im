package com.guru.im.mq.starter.core.retry;

import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;

public class RetryMessage {
    private String messageId;
    private MQMessageWrapper originalMessage;
    private int attemptCount;
    private long nextRetryTime;
    private String retryReason;
    private MQMessageType messageType;
    private String originalTopic;
    private String originalTag;

    private RetryMessage(Builder builder) {
        messageId = builder.messageId;
        originalMessage = builder.originalMessage;
        attemptCount = builder.attemptCount;
        nextRetryTime = builder.nextRetryTime;
        retryReason = builder.retryReason;
        messageType = builder.messageType;
        originalTopic = builder.originalTopic;
        originalTag = builder.originalTag;
    }

    // Redis key生成
    public String getRedisKey() {
        return String.format("retry:msg:%s", messageId);
    }
    
    // 是否应该重试
    public boolean shouldRetry() {
        return System.currentTimeMillis() >= nextRetryTime;
    }


    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MQMessageWrapper getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(MQMessageWrapper originalMessage) {
        this.originalMessage = originalMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public long getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(long nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getRetryReason() {
        return retryReason;
    }

    public void setRetryReason(String retryReason) {
        this.retryReason = retryReason;
    }

    public MQMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MQMessageType messageType) {
        this.messageType = messageType;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public void setOriginalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
    }

    public String getOriginalTag() {
        return originalTag;
    }

    public void setOriginalTag(String originalTag) {
        this.originalTag = originalTag;
    }

    public static Builder builder() {
        return Builder.newBuilder();
    }

    public static final class Builder {
        private String messageId;
        private MQMessageWrapper originalMessage;
        private int attemptCount;
        private long nextRetryTime;
        private String retryReason;
        private MQMessageType messageType;
        private String originalTopic;
        private String originalTag;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder messageId(String val) {
            messageId = val;
            return this;
        }

        public Builder originalMessage(MQMessageWrapper val) {
            originalMessage = val;
            return this;
        }

        public Builder attemptCount(int val) {
            attemptCount = val;
            return this;
        }

        public Builder nextRetryTime(long val) {
            nextRetryTime = val;
            return this;
        }

        public Builder retryReason(String val) {
            retryReason = val;
            return this;
        }

        public Builder messageType(MQMessageType val) {
            messageType = val;
            return this;
        }

        public Builder originalTopic(String val) {
            originalTopic = val;
            return this;
        }

        public Builder originalTag(String val) {
            originalTag = val;
            return this;
        }

        public RetryMessage build() {
            return new RetryMessage(this);
        }
    }
}