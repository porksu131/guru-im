package com.guru.im.user.model.vo;

public class UserConversationVO {
    private Long id;
    private Long userId;
    private Long conversationId;
    private Integer conversationType;
    private String conversationKey;
    private String showName;
    private String avatar;
    private Integer unreadCount;
    private Boolean isTop;
    private Boolean isMute;
    private String lastMessageContent;
    private Long lastMessageTime;
    private Long lastMessageSender;
    private Long lastMessageSeq;
    private String lastSenderNickname;
    private Long createTime;
    private Long updateTime;


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

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public Long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Long getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(Long lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public String getLastSenderNickname() {
        return lastSenderNickname;
    }

    public void setLastSenderNickname(String lastSenderNickname) {
        this.lastSenderNickname = lastSenderNickname;
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

    public String getConversationKey() {
        return conversationKey;
    }

    public void setConversationKey(String conversationKey) {
        this.conversationKey = conversationKey;
    }

    public Long getLastMessageSeq() {
        return lastMessageSeq;
    }

    public void setLastMessageSeq(Long lastMessageSeq) {
        this.lastMessageSeq = lastMessageSeq;
    }
}
