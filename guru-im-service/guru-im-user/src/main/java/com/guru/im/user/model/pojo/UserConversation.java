package com.guru.im.user.model.pojo;

import java.io.Serializable;

public class UserConversation implements Serializable {
    private Long id;
    private Long userId;
    private Long conversationId;
    private Integer conversationType;
    private Integer unreadCount;
    private Long lastReadSeq;
    private Boolean isTop;
    private Boolean isMute;
    private String showNickname;
    private Integer status; // 0:删除 1:正常
    private Long createTime;
    private Long updateTime;

    private String conversationKey;
    private String conversationName;
    private String conversationAvatar;
    private Long lastMessageId;
    private Long lastMessageTime;
    private String lastMessageContent;
    private Long lastMessageSender;
    private Long lastMessageSeq;

    // 非数据库字段
    private Conversation conversation;
    private Group groupInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getConversationType() {
        return conversationType;
    }

    public void setConversationType(Integer conversationType) {
        this.conversationType = conversationType;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Long getLastReadSeq() {
        return lastReadSeq;
    }

    public void setLastReadSeq(Long lastReadSeq) {
        this.lastReadSeq = lastReadSeq;
    }

    public Boolean getTop() {
        return isTop;
    }

    public void setTop(Boolean top) {
        isTop = top;
    }

    public Boolean getMute() {
        return isMute;
    }

    public void setMute(Boolean mute) {
        isMute = mute;
    }

    public String getShowNickname() {
        return showNickname;
    }

    public void setShowNickname(String showNickname) {
        this.showNickname = showNickname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public Long getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(Long lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public String getConversationKey() {
        return conversationKey;
    }

    public void setConversationKey(String conversationKey) {
        this.conversationKey = conversationKey;
    }

    public String getConversationAvatar() {
        return conversationAvatar;
    }

    public void setConversationAvatar(String conversationAvatar) {
        this.conversationAvatar = conversationAvatar;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public Long getLastMessageSeq() {
        return lastMessageSeq;
    }

    public void setLastMessageSeq(Long lastMessageSeq) {
        this.lastMessageSeq = lastMessageSeq;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Group getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(Group groupInfo) {
        this.groupInfo = groupInfo;
    }
}