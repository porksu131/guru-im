package com.guru.im.mq.starter.core.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MQMessageWrapper {
    private String messageId = UUID.randomUUID().toString();
    private long timestamp = System.currentTimeMillis();
    private String version = "1.0";

    private SourceType sourceType;
    private MQMessageType messageType;
    private MQQos qos;
    private Integer priority; //消息优先级 (0:普通 1:重要 2:紧急)

    private Long globalSeq;
    private CorrelationType correlationType; // 关联内容的类型
    private String correlationId; // 关联id

    private String targetTopic;
    private String targetTag;
    private String replyTopic;
    private String replyTag;

    private List<Long> receiverIds;
    private String deviceId;

    private List<String> offlineDeviceIds;

    private Map<String, String> properties = new HashMap<>();
    private byte[] body;


    public MQMessageWrapper() {
    }

    public MQMessageWrapper(MQMessageWrapper wrapper) {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.version = wrapper.version;

        this.sourceType = wrapper.sourceType;
        this.messageType = wrapper.messageType;
        this.qos = wrapper.qos;
        this.priority = wrapper.priority;

        this.globalSeq = wrapper.globalSeq;
        this.correlationType = wrapper.correlationType;
        this.correlationId = wrapper.correlationId;

        this.targetTopic = wrapper.targetTopic;
        this.targetTag = wrapper.targetTag;
        this.replyTopic = wrapper.replyTopic;
        this.replyTag = wrapper.replyTag;

        this.receiverIds = wrapper.receiverIds;
        this.deviceId = wrapper.deviceId;

        this.offlineDeviceIds = wrapper.offlineDeviceIds;

        this.properties = wrapper.properties;
        this.body = wrapper.body;
    }

    public byte[] getBody() {
        return body;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public MQMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MQMessageType messageType) {
        this.messageType = messageType;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public void setTargetTopic(String targetTopic) {
        this.targetTopic = targetTopic;
    }

    public String getTargetTag() {
        return targetTag;
    }

    public void setTargetTag(String targetTag) {
        this.targetTag = targetTag;
    }

    public MQQos getQos() {
        return qos;
    }

    public void setQos(MQQos qos) {
        this.qos = qos;
    }

    public String getReplyTopic() {
        return replyTopic;
    }

    public void setReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
    }

    public String getReplyTag() {
        return replyTag;
    }

    public void setReplyTag(String replyTag) {
        this.replyTag = replyTag;
    }

    public List<Long> getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(List<Long> receiverIds) {
        this.receiverIds = receiverIds;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public CorrelationType getCorrelationType() {
        return correlationType;
    }

    public void setCorrelationType(CorrelationType correlationType) {
        this.correlationType = correlationType;
    }

    public List<String> getOfflineDeviceIds() {
        return offlineDeviceIds;
    }

    public void setOfflineDeviceIds(List<String> offlineDeviceIds) {
        this.offlineDeviceIds = offlineDeviceIds;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getHandlerKey() {
        return messageId;
    }

    public Long getGlobalSeq() {
        return globalSeq;
    }

    public void setGlobalSeq(Long globalSeq) {
        this.globalSeq = globalSeq;
    }

    public String getDestination() {
        return StringUtils.isBlank(targetTag) ? targetTopic : String.format("%s:%s", targetTopic, targetTag);
    }
}