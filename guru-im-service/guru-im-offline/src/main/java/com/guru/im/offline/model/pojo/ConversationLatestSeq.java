package com.guru.im.offline.model.pojo;

public class ConversationLatestSeq {
    private Long conversationId;
    private Long latestSeq;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getLatestSeq() {
        return latestSeq;
    }

    public void setLatestSeq(Long latestSeq) {
        this.latestSeq = latestSeq;
    }
}