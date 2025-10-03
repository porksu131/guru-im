package com.guru.im.user.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.Group;
import com.guru.im.user.model.pojo.GroupMember;
import com.guru.im.user.model.vo.GroupCreateRequest;
import com.guru.im.user.model.vo.GroupInviteRequest;
import com.guru.im.user.model.vo.GroupUpdateRequest;

import java.util.List;

public interface GroupService {

    ResponseResult<Group> createGroup(GroupCreateRequest request);

    ResponseResult<Boolean> updateGroup(GroupUpdateRequest request);

    ResponseResult<Boolean> dismissGroup(Long groupId, Long operatorId);

    ResponseResult<Boolean> inviteMembers(GroupInviteRequest request);

    ResponseResult<Boolean> removeMember(Long groupId, Long operatorId, Long targetUserId);

    ResponseResult<Boolean> setAdmin(Long groupId, Long operatorId, Long targetUserId, boolean isAdmin);

    ResponseResult<Boolean> quitGroup(Long groupId, Long userId);

    ResponseResult<Group> getGroupInfo(Long groupId);

    ResponseResult<List<GroupMember>> getGroupMembers(Long groupId);

    ResponseResult<List<Group>> getUserGroups(Long userId);

    ResponseResult<Boolean> transferGroup(Long groupId, Long ownerId, Long newOwnerId);
}