package com.guru.im.single.mapper;

import com.guru.im.single.model.pojo.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {
    int updateLastMessageById(@Param("id") Long id,
                              @Param("lastMessageId") Long lastMessageId,
                              @Param("lastMessageTime") Long lastMessageTime,
                              @Param("lastMessageContent") String lastMessageContent,
                              @Param("lastMessageSender") Long lastMessageSender,
                              @Param("lastMessageSeq") Long lastMessageSeq,
                              @Param("updateTime") Long updateTime,
                              @Param("version") Long version);

    Long selectVersionById(@Param("id") Long id);

    Integer countByConversationId(@Param("id") Long id);
}