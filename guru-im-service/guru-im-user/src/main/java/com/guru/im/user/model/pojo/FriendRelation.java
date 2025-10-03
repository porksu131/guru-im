package com.guru.im.user.model.pojo;

public class FriendRelation {
    private long id; // 主键
    private long uid; // 用户id
    private long friendId; // 好友id
    private String userName; // 用户名称
    private String friendName; //  好友名称
    private Long createTime;
    private Long updateTime;
    private int relationStatus; // 1:正常 2:拉黑

    // getters and setters
    public long getId() {return id;}
    public void setId(long id) {
        this.id = id;
    }
    public long getUid() {
        return uid;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }
    public long getFriendId() {
        return friendId;
    }
    public void setFriendId(long friendId) {
        this.friendId = friendId;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getFriendName() {
        return friendName;
    }
    public void setFriendName(String friendName) {
        this.friendName = friendName;
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
}
