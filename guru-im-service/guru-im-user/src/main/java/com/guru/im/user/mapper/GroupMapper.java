package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.Group;
import com.guru.im.user.model.pojo.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GroupMapper {
    
    // 群组相关
    int insertGroup(Group group);
    int updateGroup(Group group);
    int deleteGroup(@Param("groupId") Long groupId);
    Group selectGroupById(@Param("groupId") Long groupId);
    List<Group> selectGroupsByOwner(@Param("ownerId") Long ownerId);
    int updateGroupMemberCount(@Param("groupId") Long groupId, @Param("count") Integer count);
    
    // 群成员相关
    int insertGroupMember(GroupMember member);
    int updateGroupMember(GroupMember member);
    int deleteGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    GroupMember selectGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    List<GroupMember> selectGroupMembers(@Param("groupId") Long groupId);
    List<GroupMember> selectUserGroups(@Param("userId") Long userId);
    int countGroupMembers(@Param("groupId") Long groupId);
    int updateMemberRole(@Param("groupId") Long groupId, @Param("userId") Long userId, 
                        @Param("role") Integer role);
}