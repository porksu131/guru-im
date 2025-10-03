package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.GroupInvitePojo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupInviteNotifyMapper {
    
    /**
     * 插入群聊邀请通知
     */
    int insert(GroupInvitePojo notify);
    
    /**
     * 根据ID查询
     */
    GroupInvitePojo selectById(Long id);
    
    /**
     * 根据群组ID查询
     */
    List<GroupInvitePojo> selectByGroupId(Long groupId);
    
    /**
     * 根据邀请人ID查询
     */
    List<GroupInvitePojo> selectByInviterId(Long inviterId);
    
    /**
     * 更新推送状态
     */
    int updateDeliveryStatus(@Param("id") Long id,
                             @Param("deliveryStatus") Integer deliveryStatus,
                             @Param("updateTime") Long updateTime);
    
    /**
     * 更新通知信息
     */
    int update(GroupInvitePojo notify);
    
    /**
     * 根据ID删除
     */
    int deleteById(Long id);
    
    /**
     * 查询过期的通知
     */
    List<GroupInvitePojo> selectExpiredNotifies(@Param("currentTime") Long currentTime);
    
    /**
     * 批量更新状态为过期
     */
    int batchUpdateStatusToExpired(@Param("ids") List<Long> ids, @Param("currentTime") Long currentTime);
    
    /**
     * 根据状态查询通知
     */
    List<GroupInvitePojo> selectByStatus(Integer deliveryStatus);
}