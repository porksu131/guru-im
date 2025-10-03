package com.guru.im.common.model;

import java.util.List;

public class MessageBusinessMeta {
    private String business;
    private String topic;
    private String tag;
    private int eventType;
    private long receiverId;
    private String conversationId;
    private String messageType;
    private MQMessageType mqMessageType;
    private boolean isBatch;
    List<Long> batchIds;
    private byte[] data;

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public boolean isBatch() {
        return isBatch;
    }

    public void setBatch(boolean batch) {
        isBatch = batch;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<Long> getBatchIds() {
        return batchIds;
    }

    public void setBatchIds(List<Long> batchIds) {
        this.batchIds = batchIds;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public MQMessageType getMqMessageType() {
        return mqMessageType;
    }

    public void setMqMessageType(MQMessageType mqMessageType) {
        this.mqMessageType = mqMessageType;
    }
}
