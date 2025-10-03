package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.OfflineMessageContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OfflineMessageContentMapper {

    /**
     * 插入单条消息
     */
    int insert(OfflineMessageContent message);

    /**
     * 根据用户和会话查询离线消息
     */
    List<OfflineMessageContent> findByUserAndConversation(
            @Param("userId") Long userId,
            @Param("conversationType") Integer conversationType,
            @Param("conversationId") Long conversationId,
            @Param("lastSyncSeq") Long lastSyncSeq,
            @Param("limit") Integer limit,
            @Param("currentTime") Long currentTime);

    /**
     * 批量插入离线消息
     */
    int batchInsert(@Param("list") List<OfflineMessageContent> messages);

    /**
     * 删除过期消息
     */
    int deleteExpiredMessages(@Param("currentTime") Long currentTime);

    /**
     * 删除已投递的消息
     */
    int deleteDeliveredMessages(@Param("userId") Long userId);

    /**
     * 根据消息ID查询消息
     */
    OfflineMessageContent findByMessageId(@Param("messageId") Long messageId);

    /**
     * 查询用户的所有离线消息数量
     */
    Integer countByUser(@Param("userId") Long userId);

    /**
     * 查询用户某个会话的离线消息数量
     */
    Integer countByUserAndConversation(
            @Param("userId") Long userId,
            @Param("conversationType") Integer conversationType,
            @Param("conversationId") Long conversationId,
            @Param("lastSyncSeq") Long lastSyncSeq);

    /**
     * 更新消息归档状态
     */
    int updateArchiveStatus(
            @Param("messageId") Long messageId,
            @Param("isArchived") Integer isArchived,
            @Param("archiveTime") Long archiveTime);

    /**
     * 批量更新消息过期时间
     */
    int batchUpdateExpireTime(
            @Param("messageIds") List<Long> messageIds,
            @Param("expireTime") Long expireTime);

    int deleteMessages(@Param("messageIds") List<Long> messageIds);

    List<Long> findMessagesToArchive(@Param("archiveTime") Long archiveTime);

    int archiveMessages(@Param("messageIds") List<Long> messageIds,
                        @Param("archiveTime") Long archiveTime);


    /**
     * 查询过期的消息ID列表
     */
    List<Long> selectExpiredMessageIds(@Param("archiveTime") long archiveTime);

    /**
     * 批量归档消息
     */
    int batchArchiveMessages(@Param("messageIds") List<Long> messageIds,
                             @Param("archiveTime") long archiveTime);

    /**
     * 查询需要归档的消息总数（用于分页处理）
     */
    long countExpiredMessages(@Param("archiveTime") long archiveTime);
}
