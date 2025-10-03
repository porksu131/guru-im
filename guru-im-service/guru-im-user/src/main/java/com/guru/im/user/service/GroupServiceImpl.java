package com.guru.im.user.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.user.mapper.*;
import com.guru.im.user.model.pojo.*;
import com.guru.im.user.model.vo.GroupCreateRequest;
import com.guru.im.user.model.vo.GroupInviteRequest;
import com.guru.im.user.model.vo.GroupUpdateRequest;
import com.guru.im.user.rocketmq.handler.GroupInviteNotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    @Autowired
    private UserConversationMapper userConversationMapper;
    @Autowired
    private GroupInviteNotifyService groupInviteNotifyService;

    @Override
    @Transactional
    public ResponseResult<Group> createGroup(GroupCreateRequest request) {
        try {
            // 验证群主是否存在
            User owner = userMapper.getByUid(request.getGroupOwner());
            if (owner == null) {
                return ResponseResult.fail("群主不存在");
            }

            // 验证初始成员
            if (CollectionUtils.isEmpty(request.getMemberIds())) {
                return ResponseResult.fail("初始成员不能为空");
            }

            // 检查成员数量
            if (request.getMemberIds().size() + 1 > request.getMaxMembers()) {
                return ResponseResult.fail("初始成员数量超过群最大人数限制");
            }

            // 创建群组
            Group group = new Group();
            group.setId(generateSnowflakeId());
            group.setGroupName(request.getGroupName());
            group.setGroupAvatar(request.getGroupAvatar());
            group.setGroupOwner(request.getGroupOwner());
            group.setGroupIntro(request.getGroupIntro());
            group.setGroupNotice(request.getGroupNotice());
            group.setMaxMembers(request.getMaxMembers());
            group.setCurrentMembers(request.getMemberIds().size() + 1); // 包含群主
            group.setGroupStatus(1);
            group.setOwnerInfo(owner);
            long now = System.currentTimeMillis();
            group.setCreateTime(now);
            group.setUpdateTime(now);

            groupMapper.insertGroup(group);

            // 添加群主和初始成员
            List<Long> allMemberIds = new ArrayList<>(request.getMemberIds());
            allMemberIds.add(request.getGroupOwner()); // 包含群主

            // 添加群主
            addGroupMember(group.getId(), request.getGroupOwner(), 2, now);

            // 添加初始成员
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(request.getGroupOwner())) {
                    addGroupMember(group.getId(), memberId, 0, now);
                }
            }

            // 为群组创建会话，并为所有成员创建用户会话
            ResponseResult<Conversation> conversationResult = conversationService.createGroupConversation(
                    group.getId(), group.getGroupName(), group.getGroupAvatar(), allMemberIds);

            if (ResponseResult.isError(conversationResult)) {
                throw new RuntimeException("创建群聊会话失败: " + conversationResult.getMsg());
            }


            // 推送群聊创建通知给所有成员
            groupInviteNotifyService.pushGroupInviteNotify(group, allMemberIds);

            return ResponseResult.ok("群创建成功", group);

        } catch (Exception e) {
            throw new RuntimeException("创建群组失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> updateGroup(GroupUpdateRequest request) {
        try {
            Group group = groupMapper.selectGroupById(request.getGroupId());
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 验证操作权限（只有群主或管理员可以修改）
            GroupMember operator = groupMapper.selectGroupMember(request.getGroupId(), request.getOperatorId());
            if (operator == null || operator.getMemberRole() < 1) {
                return ResponseResult.fail("无权限修改群信息");
            }

            // 更新群信息
            boolean hasUpdate = false;
            if (request.getGroupName() != null && !request.getGroupName().equals(group.getGroupName())) {
                group.setGroupName(request.getGroupName());
                hasUpdate = true;
            }
            if (request.getGroupAvatar() != null && !request.getGroupAvatar().equals(group.getGroupAvatar())) {
                group.setGroupAvatar(request.getGroupAvatar());
                hasUpdate = true;
            }
            if (request.getGroupIntro() != null && !request.getGroupIntro().equals(group.getGroupIntro())) {
                group.setGroupIntro(request.getGroupIntro());
                hasUpdate = true;
            }
            if (request.getGroupNotice() != null && !request.getGroupNotice().equals(group.getGroupNotice())) {
                group.setGroupNotice(request.getGroupNotice());
                hasUpdate = true;
            }
            if (request.getMaxMembers() != null && !request.getMaxMembers().equals(group.getMaxMembers())) {
                if (request.getMaxMembers() < group.getCurrentMembers()) {
                    return ResponseResult.fail("最大成员数不能小于当前成员数");
                }
                group.setMaxMembers(request.getMaxMembers());
                hasUpdate = true;
            }

            if (hasUpdate) {
                group.setUpdateTime(System.currentTimeMillis());
                groupMapper.updateGroup(group);

                // 同步更新会话信息
                updateGroupConversationInfo(group.getId(), group.getGroupName(), group.getGroupAvatar());
            }

            return ResponseResult.ok("群信息更新成功", true);

        } catch (Exception e) {
            throw new RuntimeException("更新群信息失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> dismissGroup(Long groupId, Long operatorId) {
        try {
            Group group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 只有群主可以解散群
            if (!group.getGroupOwner().equals(operatorId)) {
                return ResponseResult.fail("只有群主可以解散群");
            }

            // 解散群组（软删除）
            group.setGroupStatus(0);
            group.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroup(group);

            // 将所有成员状态设置为已退出
            List<GroupMember> members = groupMapper.selectGroupMembers(groupId);
            long now = System.currentTimeMillis();
            for (GroupMember member : members) {
                member.setMemberStatus(0);
                member.setUpdateTime(now);
                groupMapper.updateGroupMember(member);
            }

            // 禁用相关会话（可选）
            disableGroupConversation(groupId);

            return ResponseResult.ok("群解散成功", true);

        } catch (Exception e) {
            throw new RuntimeException("解散群失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> inviteMembers(GroupInviteRequest request) {
        try {
            Group group = groupMapper.selectGroupById(request.getGroupId());
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 验证操作人权限（群主或管理员可以邀请）
            GroupMember operator = groupMapper.selectGroupMember(request.getGroupId(), request.getOperatorId());
            if (operator == null || operator.getMemberRole() < 1) {
                return ResponseResult.fail("无权限邀请成员");
            }

            // 检查群人数限制
            int currentCount = groupMapper.countGroupMembers(request.getGroupId());
            if (currentCount + request.getInviteUserIds().size() > group.getMaxMembers()) {
                return ResponseResult.fail("超出群成员人数限制");
            }

            long now = System.currentTimeMillis();
            List<Long> newMemberIds = new ArrayList<>();

            for (Long userId : request.getInviteUserIds()) {
                // 验证用户是否存在
                User user = userMapper.getByUid(userId);
                if (user == null) {
                    continue; // 跳过不存在的用户
                }

                // 检查是否已是成员
                GroupMember existing = groupMapper.selectGroupMember(request.getGroupId(), userId);
                if (existing != null) {
                    if (existing.getMemberStatus() == 0) {
                        // 重新加入
                        existing.setMemberStatus(1);
                        existing.setUpdateTime(now);
                        groupMapper.updateGroupMember(existing);
                        newMemberIds.add(userId);
                    }
                    continue;
                }

                // 添加新成员
                addGroupMember(request.getGroupId(), userId, 0, now);
                newMemberIds.add(userId);
            }

            if (!newMemberIds.isEmpty()) {
                // 更新群成员数量
                group.setCurrentMembers(currentCount + newMemberIds.size());
                group.setUpdateTime(now);
                groupMapper.updateGroup(group);

                // 为新成员创建用户会话
                Conversation conversation = conversationMapper.selectConversationByKey("group_" + request.getGroupId());
                if (conversation != null) {
                    for (Long userId : newMemberIds) {
                        createUserConversation(userId, conversation, group.getGroupName(), now);
                    }
                }
            }

            return ResponseResult.ok("邀请成功", true);

        } catch (Exception e) {
            throw new RuntimeException("邀请成员失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> removeMember(Long groupId, Long operatorId, Long targetUserId) {
        try {
            Group group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 验证操作人权限
            GroupMember operator = groupMapper.selectGroupMember(groupId, operatorId);
            if (operator == null) {
                return ResponseResult.fail("操作人不是群成员");
            }

            // 验证目标成员
            GroupMember targetMember = groupMapper.selectGroupMember(groupId, targetUserId);
            if (targetMember == null || targetMember.getMemberStatus() == 0) {
                return ResponseResult.fail("目标用户不是群成员");
            }

            // 权限检查：群主可以移除任何人，管理员只能移除普通成员
            if (operator.getMemberRole() == 2) { // 群主
                // 群主可以移除任何人
            } else if (operator.getMemberRole() == 1) { // 管理员
                if (targetMember.getMemberRole() >= 1) { // 不能移除管理员或群主
                    return ResponseResult.fail("管理员只能移除普通成员");
                }
            } else {
                return ResponseResult.fail("无权限移除成员");
            }

            // 不能移除自己（需要通过退出群聊）
            if (targetUserId.equals(operatorId)) {
                return ResponseResult.fail("请使用退出群聊功能");
            }

            // 移除成员（软删除）
            targetMember.setMemberStatus(0);
            targetMember.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroupMember(targetMember);

            // 更新群成员数量
            group.setCurrentMembers(group.getCurrentMembers() - 1);
            group.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroup(group);

            // 删除用户的会话（可选，或者标记为已删除）
            deleteUserConversation(targetUserId, groupId);

            return ResponseResult.ok("成员移除成功", true);

        } catch (Exception e) {
            throw new RuntimeException("移除成员失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> setAdmin(Long groupId, Long operatorId, Long targetUserId, boolean isAdmin) {
        try {
            Group group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 只有群主可以设置管理员
            if (!group.getGroupOwner().equals(operatorId)) {
                return ResponseResult.fail("只有群主可以设置管理员");
            }

            // 不能设置自己（群主已经是最高权限）
            if (targetUserId.equals(operatorId)) {
                return ResponseResult.fail("群主无需设置管理员权限");
            }

            // 验证目标成员
            GroupMember targetMember = groupMapper.selectGroupMember(groupId, targetUserId);
            if (targetMember == null || targetMember.getMemberStatus() == 0) {
                return ResponseResult.fail("目标用户不是群成员");
            }

            Integer newRole = isAdmin ? 1 : 0; // 1:管理员, 0:普通成员
            if (targetMember.getMemberRole().equals(newRole)) {
                return ResponseResult.fail("用户已经是" + (isAdmin ? "管理员" : "普通成员"));
            }

            groupMapper.updateMemberRole(groupId, targetUserId, newRole);

            return ResponseResult.ok((isAdmin ? "设置" : "取消") + "管理员成功", true);

        } catch (Exception e) {
            throw new RuntimeException("设置管理员失败", e);
        }
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> quitGroup(Long groupId, Long userId) {
        try {
            Group group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 验证成员身份
            GroupMember member = groupMapper.selectGroupMember(groupId, userId);
            if (member == null || member.getMemberStatus() == 0) {
                return ResponseResult.fail("您不是群成员");
            }

            // 群主不能直接退出，需要先转让群主或解散群
            if (member.getMemberRole() == 2) {
                return ResponseResult.fail("群主不能直接退出，请先转让群主或解散群");
            }

            // 退出群聊（软删除）
            member.setMemberStatus(0);
            member.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroupMember(member);

            // 更新群成员数量
            group.setCurrentMembers(group.getCurrentMembers() - 1);
            group.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroup(group);

            // 删除用户的会话
            deleteUserConversation(userId, groupId);

            return ResponseResult.ok("退出群聊成功", true);

        } catch (Exception e) {
            throw new RuntimeException("退出群聊失败", e);
        }
    }

    @Override
    public ResponseResult<Group> getGroupInfo(Long groupId) {
        Group group = groupMapper.selectGroupById(groupId);
        if (group == null) {
            return ResponseResult.fail("群组不存在");
        }

        // 填充群主信息
        User owner = userMapper.getByUid(group.getGroupOwner());
        if (owner != null) {
            User ownerInfo = new User();
            ownerInfo.setUid(owner.getUid());
            ownerInfo.setUserName(owner.getUserName());
            ownerInfo.setAvatar(owner.getAvatar());
            group.setOwnerInfo(ownerInfo);
        }

        // 填充成员列表
        List<GroupMember> members = groupMapper.selectGroupMembers(groupId);
        group.setMembers(members);

        return ResponseResult.ok(group);
    }

    @Override
    public ResponseResult<List<GroupMember>> getGroupMembers(Long groupId) {
        List<GroupMember> members = groupMapper.selectGroupMembers(groupId);
        if (members != null) {
            // 为每个成员填充用户信息
            for (GroupMember member : members) {
                User user = userMapper.getByUid(member.getUserId());
                if (user != null) {
                    // 创建UserInfo对象（需要新建这个类）
                    User userInfo = new User();
                    userInfo.setUid(user.getUid());
                    userInfo.setUserName(user.getUserName());
                    userInfo.setAvatar(user.getAvatar());
                    userInfo.setPhone(user.getPhone());
                    member.setUserInfo(userInfo);

                    // 设置显示名称
                    member.setShowName(user.getUserName());
                }
            }
        } else {
            members = new ArrayList<>();
        }
        return ResponseResult.ok(members);
    }

    @Override
    public ResponseResult<List<Group>> getUserGroups(Long userId) {
        List<GroupMember> userGroups = groupMapper.selectUserGroups(userId);
        List<Group> groups = new ArrayList<>();
        for (GroupMember member : userGroups) {
            Group group = groupMapper.selectGroupById(member.getGroupId());
            if (group != null && group.getGroupStatus() == 1) {
                // 填充群主信息
                User owner = userMapper.getByUid(group.getGroupOwner());
                if (owner != null) {
                    User ownerInfo = new User();
                    ownerInfo.setUid(owner.getUid());
                    ownerInfo.setUserName(owner.getUserName());
                    ownerInfo.setAvatar(owner.getAvatar());
                    group.setOwnerInfo(ownerInfo);
                }

                // 填充成员数量（实时查询）
                int memberCount = groupMapper.countGroupMembers(group.getId());
                group.setCurrentMembers(memberCount);

                groups.add(group);
            }
        }
        return ResponseResult.ok(groups);
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> transferGroup(Long groupId, Long ownerId, Long newOwnerId) {
        try {
            Group group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return ResponseResult.fail("群组不存在");
            }

            // 验证当前群主身份
            if (!group.getGroupOwner().equals(ownerId)) {
                return ResponseResult.fail("只有群主可以转让群");
            }

            // 不能转让给自己
            if (ownerId.equals(newOwnerId)) {
                return ResponseResult.fail("不能转让给自己");
            }

            // 验证新群主是否是群成员
            GroupMember newOwnerMember = groupMapper.selectGroupMember(groupId, newOwnerId);
            if (newOwnerMember == null || newOwnerMember.getMemberStatus() == 0) {
                return ResponseResult.fail("新群主不是群成员");
            }

            // 转让群主
            group.setGroupOwner(newOwnerId);
            group.setUpdateTime(System.currentTimeMillis());
            groupMapper.updateGroup(group);

            // 更新原群主角色为普通成员
            groupMapper.updateMemberRole(groupId, ownerId, 0);

            // 更新新群主角色为群主
            groupMapper.updateMemberRole(groupId, newOwnerId, 2);

            return ResponseResult.ok("群主转让成功", true);

        } catch (Exception e) {
            throw new RuntimeException("转让群主失败", e);
        }
    }


    // ============ 辅助方法 ============

    private void addGroupMember(Long groupId, Long userId, Integer role, Long joinTime) {
        GroupMember member = new GroupMember();
        member.setId(generateSnowflakeId());
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setMemberRole(role);
        member.setJoinTime(joinTime);
        member.setLastAckSeq(0L);
        member.setMemberStatus(1);
        member.setCreateTime(joinTime);
        member.setUpdateTime(joinTime);

        groupMapper.insertGroupMember(member);
    }

    private void createUserConversation(Long userId, Conversation conversation, String showNickname, long createTime) {
        UserConversation userConversation = new UserConversation();
        userConversation.setId(generateSnowflakeId());
        userConversation.setUserId(userId);
        userConversation.setConversationId(conversation.getId());
        userConversation.setConversationType(conversation.getConversationType());
        userConversation.setUnreadCount(0);
        userConversation.setLastReadSeq(0L);
        userConversation.setTop(false);
        userConversation.setMute(false);
        userConversation.setShowNickname(showNickname);
        userConversation.setStatus(1);
        userConversation.setCreateTime(createTime);
        userConversation.setUpdateTime(createTime);

        userConversationMapper.insertUserConversation(userConversation);
    }

    private void updateGroupConversationInfo(Long groupId, String groupName, String groupAvatar) {
        try {
            Conversation conversation = conversationMapper.selectConversationByKey("group_" + groupId);
            if (conversation != null) {
                conversation.setConversationName(groupName);
                conversation.setConversationAvatar(groupAvatar);
                conversation.setUpdateTime(System.currentTimeMillis());
                conversationMapper.updateConversation(conversation);
            }
        } catch (Exception e) {
            // 日志记录错误，但不影响主流程
            System.err.println("更新群会话信息失败: " + e.getMessage());
        }
    }

    private void disableGroupConversation(Long groupId) {
        try {
            Conversation conversation = conversationMapper.selectConversationByKey("group_" + groupId);
            if (conversation != null) {
                conversation.setStatus(0);
                conversation.setUpdateTime(System.currentTimeMillis());
                conversationMapper.updateConversation(conversation);
            }
        } catch (Exception e) {
            System.err.println("禁用群会话失败: " + e.getMessage());
        }
    }

    private void deleteUserConversation(Long userId, Long groupId) {
        try {
            Conversation conversation = conversationMapper.selectConversationByKey("group_" + groupId);
            if (conversation != null) {
                UserConversation userConversation = userConversationMapper.selectUserConversation(userId, conversation.getId());
                if (userConversation != null) {
                    userConversation.setStatus(0);
                    userConversation.setUpdateTime(System.currentTimeMillis());
                    userConversationMapper.updateUserConversation(userConversation);
                }
            }
        } catch (Exception e) {
            System.err.println("删除用户会话失败: " + e.getMessage());
        }
    }

    private Long generateSnowflakeId() {
        return idGenerator.nextId();
    }
}