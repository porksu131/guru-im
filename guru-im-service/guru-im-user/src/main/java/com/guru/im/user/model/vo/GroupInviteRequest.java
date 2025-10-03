package com.guru.im.user.model.vo;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GroupInviteRequest {
    @NotNull(message = "群ID不能为空")
    private Long groupId;
    
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
    
    @NotNull(message = "被邀请用户列表不能为空")
    private List<Long> inviteUserIds;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public List<Long> getInviteUserIds() {
        return inviteUserIds;
    }

    public void setInviteUserIds(List<Long> inviteUserIds) {
        this.inviteUserIds = inviteUserIds;
    }
}