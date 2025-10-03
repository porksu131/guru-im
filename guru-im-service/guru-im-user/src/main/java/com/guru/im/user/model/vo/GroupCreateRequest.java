package com.guru.im.user.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GroupCreateRequest {
    @NotBlank(message = "群名称不能为空")
    private String groupName;
    
    private String groupAvatar;
    
    @NotNull(message = "群主ID不能为空")
    private Long groupOwner;
    
    private String groupIntro;
    private String groupNotice;
    
    private Integer maxMembers = 500;
    
    @NotNull(message = "初始成员不能为空")
    private List<Long> memberIds;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public void setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
    }

    public Long getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(Long groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getGroupIntro() {
        return groupIntro;
    }

    public void setGroupIntro(String groupIntro) {
        this.groupIntro = groupIntro;
    }

    public String getGroupNotice() {
        return groupNotice;
    }

    public void setGroupNotice(String groupNotice) {
        this.groupNotice = groupNotice;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}