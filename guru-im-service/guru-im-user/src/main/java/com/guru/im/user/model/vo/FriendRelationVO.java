package com.guru.im.user.model.vo;

public class FriendRelationVO {
    private Long id;
    private Long uid;
    private Long friendId;
    private String friendName;
    private int relationStatus; // 1:正常 2:拉黑 3:删除
    private Long createTime;
    private Long updateTime;
    private Integer onlineStatus;  // 在线状态
    private Long lastActiveTime; // 最后活跃时间
    private UserConversationVO userConversation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public int getRelationStatus() {
        return relationStatus;
    }

    public void setRelationStatus(int relationStatus) {
        this.relationStatus = relationStatus;
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

    public UserConversationVO getUserConversation() {
        return userConversation;
    }

    public void setUserConversation(UserConversationVO userConversation) {
        this.userConversation = userConversation;
    }
}