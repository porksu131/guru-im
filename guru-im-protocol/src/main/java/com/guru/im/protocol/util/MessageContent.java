package com.guru.im.protocol.util;

import com.guru.im.protocol.model.ChatMessageType;

import java.util.Map;

public class MessageContent {
    private ChatMessageType messageType;
    private Map<String, Object> content;
    private String summary; // 用于会话列表显示

    public ChatMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(ChatMessageType messageType) {
        this.messageType = messageType;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
