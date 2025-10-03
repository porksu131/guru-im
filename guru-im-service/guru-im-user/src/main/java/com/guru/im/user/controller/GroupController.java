package com.guru.im.user.controller;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.Group;
import com.guru.im.user.model.pojo.GroupMember;
import com.guru.im.user.model.vo.GroupCreateRequest;
import com.guru.im.user.model.vo.GroupInviteRequest;
import com.guru.im.user.model.vo.GroupUpdateRequest;
import com.guru.im.user.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    public ResponseResult<Group> createGroup(@Validated @RequestBody GroupCreateRequest request) {
        return groupService.createGroup(request);
    }

    @PostMapping("/update")
    public ResponseResult<Boolean> updateGroup(@Validated @RequestBody GroupUpdateRequest request) {
        return groupService.updateGroup(request);
    }

    @PostMapping("/invite")
    public ResponseResult<Boolean> inviteMembers(@Validated @RequestBody GroupInviteRequest request) {
        return groupService.inviteMembers(request);
    }

    @PostMapping("/dismiss")
    public ResponseResult<Boolean> dismissGroup(@RequestParam Long groupId, 
                                               @RequestParam Long operatorId) {
        return groupService.dismissGroup(groupId, operatorId);
    }

    @GetMapping("/info/{groupId}")
    public ResponseResult<Group> getGroupInfo(@PathVariable Long groupId) {
        return groupService.getGroupInfo(groupId);
    }

    @GetMapping("/members/{groupId}")
    public ResponseResult<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        return groupService.getGroupMembers(groupId);
    }

    @PostMapping("/list")
    public ResponseResult<List<Group>> getUserGroups(@RequestBody Map<String, Long> reqMap) {
        Long uid = reqMap.get("uid");
        return groupService.getUserGroups(uid);
    }

    @PostMapping("/removeMember")
    public ResponseResult<Boolean> removeMember(@RequestParam Long groupId,
                                               @RequestParam Long operatorId,
                                               @RequestParam Long targetUserId) {
        return groupService.removeMember(groupId, operatorId, targetUserId);
    }

    @PostMapping("/setAdmin")
    public ResponseResult<Boolean> setAdmin(@RequestParam Long groupId,
                                           @RequestParam Long operatorId,
                                           @RequestParam Long targetUserId,
                                           @RequestParam Boolean isAdmin) {
        return groupService.setAdmin(groupId, operatorId, targetUserId, isAdmin);
    }

    @PostMapping("/quit")
    public ResponseResult<Boolean> quitGroup(@RequestParam Long groupId,
                                            @RequestParam Long userId) {
        return groupService.quitGroup(groupId, userId);
    }
}