package com.guru.im.single.mapper;

import com.guru.im.single.model.dto.ConversationKey;
import com.guru.im.single.model.pojo.ConversationRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationReadMapper {

    /**
     * 插入或更新已读位置
     */
    int upsertReadPosition(ConversationRead conversationRead);

    /**
     * 乐观锁更新已读位置
     */
    int updateReadPositionWithLock(@Param("userId") Long userId,
                                  @Param("conversationType") Integer conversationType,
                                  @Param("conversationId") String conversationId,
                                  @Param("newSeq") Long newSeq,
                                  @Param("updateTime") Long updateTime);

    /**
     * 查询用户的会话已读位置
     */
    ConversationRead selectByUserAndConversation(@Param("userId") Long userId,
                                                @Param("conversationType") Integer conversationType,
                                                @Param("conversationId") Long conversationId);

    /**
     * 查询用户所有会话的已读位置
     */
    List<ConversationRead> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询会话中所有用户的已读位置
     */
    List<ConversationRead> selectByConversation(@Param("conversationType") Integer conversationType,
                                               @Param("conversationId") String conversationId);

    /**
     * 批量查询用户的会话已读位置
     */
    List<ConversationRead> selectByUserAndConversations(@Param("userId") Long userId,
                                                       @Param("conversations") List<ConversationKey> conversations);

    /**
     * 删除会话已读记录
     */
    int deleteByConversation(@Param("conversationType") Integer conversationType,
                            @Param("conversationId") String conversationId);

    int updateDeliveryStatus(@Param("id") Long id,
                             @Param("deliveryStatus") Integer deliveryStatus,
                             @Param("deliveryTime") Long deliveryTime,
                             @Param("updateTime") Long updateTime);
}