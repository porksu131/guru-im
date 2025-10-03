package com.guru.im.disaptch.rocketmq.retry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.common.model.MQQos;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DispatchRetryContext {
    private String messageId;
    private Long uid;
    private MQMessageWrapper messageContent;
    private List<DeviceStatus> targetGatewayNodes; // 目标网关节点列表
    private List<DeviceStatus> failedGatewayNodes; // 失败的网关节点列表
    private int retryCount;
    private Long nextRetryTime;
    private Long expiryTime; // 重试过期时间
    private MQMessageType messageType;
    private MQQos qos;
    private RetryResultHandler retryResultHandler;

    public String getRedisKey() {
        return String.format("push_retry:%s:%s", uid, messageId);
    }
    
    public boolean shouldRetry() {
        return System.currentTimeMillis() >= nextRetryTime && 
               System.currentTimeMillis() < expiryTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }


    public String getMessageId() {
        return messageId;
    }

    public Long getUid() {
        return uid;
    }

    public MQMessageWrapper getMessageContent() {
        return messageContent;
    }

    public List<DeviceStatus> getTargetGatewayNodes() {
        return targetGatewayNodes;
    }

    public List<DeviceStatus> getFailedGatewayNodes() {
        return failedGatewayNodes;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getNextRetryTime() {
        return nextRetryTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public MQMessageType getMessageType() {
        return messageType;
    }

    public MQQos getQos() {
        return qos;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public void setMessageContent(MQMessageWrapper messageContent) {
        this.messageContent = messageContent;
    }

    public void setTargetGatewayNodes(List<DeviceStatus> targetGatewayNodes) {
        this.targetGatewayNodes = targetGatewayNodes;
    }

    public void setFailedGatewayNodes(List<DeviceStatus> failedGatewayNodes) {
        this.failedGatewayNodes = failedGatewayNodes;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setNextRetryTime(Long nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setMessageType(MQMessageType messageType) {
        this.messageType = messageType;
    }

    public void setQos(MQQos qos) {
        this.qos = qos;
    }

    public RetryResultHandler getRetryResultHandler() {
        return retryResultHandler;
    }

    public void setRetryResultHandler(RetryResultHandler retryResultHandler) {
        this.retryResultHandler = retryResultHandler;
    }

}