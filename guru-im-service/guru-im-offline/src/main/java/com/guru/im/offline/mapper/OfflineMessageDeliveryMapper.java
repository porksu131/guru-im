package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.OfflineMessageDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OfflineMessageDeliveryMapper {

    /**
     * 根据ID查询投递记录
     */
    OfflineMessageDelivery selectById(@Param("id") Long id);

    /**
     * 插入单条投递记录
     */
    int insert(OfflineMessageDelivery delivery);

    /**
     * 批量插入投递记录
     */
    int batchInsert(@Param("list") List<OfflineMessageDelivery> deliveries);

    /**
     * 更新投递状态
     */
    int updateDeliveryStatus(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("deliveryStatus") Integer deliveryStatus,
            @Param("updateTime") Long updateTime);

    /**
     * 批量更新投递状态
     */
    int batchUpdateDeliveryStatus(
            @Param("deviceId") String deviceId,
            @Param("messageIds") List<Long> messageIds,
            @Param("deliveryStatus") Integer deliveryStatus,
            @Param("updateTime") Long updateTime);


    int batchUpdateDeliveryStatusByMap(
            @Param("statusMap") Map<Long, Integer> statusMap,
            @Param("updateTime") Long updateTime);


    /**
     * 查询未投递的消息
     */
    List<OfflineMessageDelivery> findUndeliveredMessages(
            @Param("userId") Long userId,
            @Param("maxRetryCount") Integer maxRetryCount,
            @Param("limit") Integer limit);

    /**
     * 根据消息ID和用户查询投递记录
     */
    List<OfflineMessageDelivery> findByMessageIdAndUser(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId);

    /**
     * 查询用户的投递记录
     */
    List<OfflineMessageDelivery> findByUser(
            @Param("userId") Long userId,
            @Param("deliveryStatus") Integer deliveryStatus);

    /**
     * 查询设备和用户的投递记录
     */
    List<OfflineMessageDelivery> findByUserAndDevice(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("deliveryStatus") Integer deliveryStatus);

    /**
     * 统计用户的未投递消息数量
     */
    Integer countUndeliveredByUser(@Param("userId") Long userId);

    /**
     * 批量删除投递记录
     */
    int batchDelete(@Param("ids") List<Long> ids);

    /**
     * 根据消息ID删除投递记录
     */
    int deleteByMessageId(@Param("messageId") Long messageId);

    /**
     * 更新投递次数
     */
    int updateDeliveryCount(
            @Param("id") Long id,
            @Param("deliveryCount") Integer deliveryCount,
            @Param("lastDeliveryTime") Long lastDeliveryTime);

    int updateDeviceDeliveryStatus(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("deviceDeliveryStatus") Integer deviceDeliveryStatus,
            @Param("updateTime") Long updateTime);

    List<OfflineMessageDelivery> findByMessageId(@Param("messageId") Long messageId);

    int deleteByMessageIds(@Param("messageIds") List<Long> messageIds);

    List<OfflineMessageDelivery> selectUndeliveredByUserAndDevice(@Param("userId") Long userId,
                                                                  @Param("deviceId") String deviceId,
                                                                  @Param("messageType") Integer messageType);

    int updateDeliveryStatusBatch(@Param("ids") List<Long> ids,
                                  @Param("status") Integer status,
                                  @Param("errorReason") String errorReason,
                                  @Param("updateTime") Long updateTime);
}