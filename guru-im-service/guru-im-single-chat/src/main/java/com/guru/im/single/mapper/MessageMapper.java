package com.guru.im.single.mapper;

import com.guru.im.single.model.pojo.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insert(Message message);

    int update(Message message);

    int recallMessage(@Param("id") Long id, @Param("recallTime") Long recallTime);

    Message selectById(@Param("id") Long id);

    List<Message> selectByConversation(@Param("conversationType") Integer conversationType,
                                      @Param("conversationId") String conversationId,
                                      @Param("startSeq") Long startSeq,
                                      @Param("limit") Integer limit);

    List<Message> selectBySender(@Param("senderId") Long senderId,
                                @Param("startTime") Long startTime,
                                @Param("endTime") Long endTime);

    Long getMaxServerSeq(@Param("conversationType") Integer conversationType,
                        @Param("conversationId") Long conversationId);

    int updateReadCount(@Param("id") Long id, @Param("increment") Integer increment);

    int batchInsert(@Param("messages") List<Message> messages);

    /**
     * 根据序列号范围更新已读计数
     */
    int incrementReadCountBySeqRange(@Param("conversationType") Integer conversationType,
                                     @Param("conversationId") Long conversationId,
                                     @Param("userId") Long userId,
                                     @Param("startSeq") Long startSeq,
                                     @Param("endSeq") Long endSeq,
                                     @Param("updateTime") Long updateTime);

    /**
     * 获取需要更新已读计数的消息ID列表
     */
    List<Long> selectUnreadMessagesBySeq(@Param("conversationType") Integer conversationType,
                                         @Param("conversationId") Long conversationId,
                                         @Param("userId") Long userId,
                                         @Param("startSeq") Long startSeq,
                                         @Param("endSeq") Long endSeq);

    /**
     * 批量更新消息的已读计数
     */
    int batchIncrementReadCount(@Param("messageIds") List<Long> messageIds,
                                @Param("updateTime") Long updateTime);


    /**
     * 获取最近非当前用户的消息发送方
     */
    Long selectRecentSenderExcludeUser(@Param("conversationType") Integer conversationType,
                                       @Param("conversationId") Long conversationId,
                                       @Param("excludeUserId") Long excludeUserId);


    /**
     * 更新推送状态
     */
    int updateDeliveryStatus(@Param("id") Long id,
                             @Param("deliveryStatus") Integer deliveryStatus,
                                @Param("deliveryTime") Long deliveryTime,
                                @Param("updateTime") Long updateTime);

    /**
     * 更新推送状态和重试信息
     */
    int updateDeliveryStatusForRetry(@Param("id") Long id,
                             @Param("deliveryStatus") Integer deliveryStatus,
                             @Param("deliveryTime") Long deliveryTime,
                             @Param("retryCount") Integer retryCount,
                             @Param("lastRetryTime") Long lastRetryTime,
                             @Param("updateTime") Long updateTime);
}