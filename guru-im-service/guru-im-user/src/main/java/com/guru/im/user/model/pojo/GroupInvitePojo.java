package com.guru.im.user.model.pojo;

public class GroupInvitePojo {
    private Long id;
    private Long groupId;
    private Long inviterId;
    private String inviteReason;
    private String initialMembersJson;
    private Integer deliveryStatus;
    private Long globalSeq;
    private Long expireTime;
    private Long createTime;
    private Long updateTime;

    // 构造函数
    public GroupInvitePojo() {}

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }

    public String getInviteReason() { return inviteReason; }
    public void setInviteReason(String inviteReason) { this.inviteReason = inviteReason; }

    public String getInitialMembersJson() { return initialMembersJson; }
    public void setInitialMembersJson(String initialMembersJson) { this.initialMembersJson = initialMembersJson; }

    public Integer getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(Integer deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public Long getExpireTime() { return expireTime; }
    public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }

    public Long getGlobalSeq() { return globalSeq; }
    public void setGlobalSeq(Long globalSeq) { this.globalSeq = globalSeq; }

    // 状态常量
    public static class DeliveryStatus {
        public static final int NOT_SENT = 0;      // 未推送
        public static final int PENDING = 1;       // 已推送，待送达
        public static final int FAILED = 2;        // 推送失败
        public static final int DELIVERED = 3;     // 已送达
    }
}