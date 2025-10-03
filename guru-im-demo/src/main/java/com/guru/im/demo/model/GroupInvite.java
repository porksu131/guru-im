package com.guru.im.demo.model;

import com.guru.im.protocol.model.RequestStatus;

import java.util.ArrayList;
import java.util.List;

public class GroupInvite {
    private Long id;
    private long groupId;
    private long inviterId;
    private String inviteReason;
    private RequestStatus requestStatus;
    private List<Long> initialMembers; // 初始成员列表
    private String groupName;
    private String groupAvatar;
    private String inviterName;
    private long globalSeq;
    private boolean isRead;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
    
    public long getInviterId() {
        return inviterId;
    }
    
    public void setInviterId(long inviterId) {
        this.inviterId = inviterId;
    }
    
    public String getInviteReason() {
        return inviteReason;
    }
    
    public void setInviteReason(String inviteReason) {
        this.inviteReason = inviteReason;
    }
    
    public RequestStatus getRequestStatus() {
        return requestStatus;
    }
    
    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }
    
    public List<Long> getInitialMembers() {
        return initialMembers;
    }
    
    public void setInitialMembers(List<Long> initialMembers) {
        this.initialMembers = initialMembers != null ? initialMembers : new ArrayList<>();
    }
    
    public void addInitialMember(long memberId) {
        this.initialMembers.add(memberId);
    }
    
    public void addAllInitialMembers(List<Long> memberIds) {
        if(initialMembers == null) {
            initialMembers = new ArrayList<>();
        }
        if (memberIds != null) {
            this.initialMembers.addAll(memberIds);
        }
    }
    
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
    
    public String getInviterName() {
        return inviterName;
    }
    
    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }
    
    public long getGlobalSeq() {
        return globalSeq;
    }
    
    public void setGlobalSeq(long globalSeq) {
        this.globalSeq = globalSeq;
    }
    
    public boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
}