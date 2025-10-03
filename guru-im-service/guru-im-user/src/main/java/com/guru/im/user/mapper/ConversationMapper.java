package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {

    int insert(Conversation conversation);

    int update(Conversation conversation);

    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessageId") Long lastMessageId,
                          @Param("lastMessageContent") String lastMessageContent,
                          @Param("lastMessageTime") Long lastMessageTime,
                          @Param("lastMessageSender") Long lastMessageSender);

    int deleteById(@Param("id") Long id);

    Conversation selectById(@Param("id") Long id);

    Conversation selectByConversationKey(@Param("conversationKey") String conversationKey);

    List<Conversation> selectByIds(@Param("ids") List<Long> ids);

    List<Conversation> selectByType(@Param("conversationType") Integer conversationType,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    int updateConversationName(@Param("id") Long id,
                               @Param("conversationName") String conversationName);

    int updateConversationAvatar(@Param("id") Long id,
                                 @Param("conversationAvatar") String conversationAvatar);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status);

    List<Conversation> selectByLastMessageTime(@Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               @Param("limit") Integer limit);

    int countByType(@Param("conversationType") Integer conversationType);

    int updateLastMessageById(@Param("id") Long id,
                              @Param("lastMessageId") Long messageId,
                              @Param("lastMessageTime") Long messageTime,
                              @Param("lastMessageContent") String messageContent,
                              @Param("lastMessageSender") Long senderId,
                              @Param("updateTime") Long updateTime,
                              @Param("version") Long currentVersion);

    Long selectVersionById(@Param("id") Long id);

    Integer countByConversationId(@Param("id") Long id);


    /**
     * 批量查询会话信息
     */
    List<Conversation> selectBatchWithUserInfo(@Param("conversationIds") List<Long> conversationIds);

    // 会话相关
    int insertConversation(Conversation conversation);
    Conversation selectConversationByKey(@Param("conversationKey") String conversationKey);
    Conversation selectConversationById(@Param("conversationId") Long conversationId);
    int updateConversation(Conversation conversation);
}