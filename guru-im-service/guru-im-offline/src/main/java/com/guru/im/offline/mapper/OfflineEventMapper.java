package com.guru.im.offline.mapper;

import com.guru.im.offline.model.dto.UserSequence;
import com.guru.im.offline.model.pojo.OfflineEventContent;
import com.guru.im.offline.model.pojo.OfflineEventDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OfflineEventMapper {

    // 批量插入事件
    int batchInsertEvents(@Param("events") List<OfflineEventContent> events);

    // 查询用户事件（基于序列号范围）
    List<OfflineEventContent> selectEventsByRange(@Param("userId") Long userId,
                                                  @Param("lastSequence") Long lastSequence,
                                                  @Param("limit") Integer limit);

    // 获取用户最大序列号
    Long selectMaxSequence(@Param("userId") Long userId);

    // 更新投递状态
    int updateDeliveryStatus(@Param("userId") Long userId,
                             @Param("deviceId") String deviceId,
                             @Param("lastSyncSeq") Long lastSyncSeq,
                             @Param("deliveryStatus") Integer deliveryStatus);

    int updateEventsDelivery(OfflineEventDelivery delivery);

    // 插入投递记录
    int insertDeliveryRecord(OfflineEventDelivery delivery);

    // 删除过期事件
    int deleteExpiredEvents(@Param("expireTime") Long expireTime,
                            @Param("batchSize") Integer batchSize);

    // 根据用户ID和设备ID查询投递记录
    OfflineEventDelivery selectDeliveryRecord(@Param("userId") Long userId,
                                              @Param("deviceId") String deviceId);

    // 批量更新投递状态
    int batchUpdateDeliveryStatus(@Param("records") List<OfflineEventDelivery> records);

    // 查询需要重新投递的记录
    List<OfflineEventDelivery> selectPendingDeliveryRecords(@Param("limit") Integer limit);

    // 获取用户的事件数量统计
    Map<String, Object> selectEventStatsByUser(@Param("userId") Long userId);

    // 批量查询用户的最大序列号
    List<UserSequence> batchSelectMaxSequence(@Param("userIds") List<Long> userIds);
}