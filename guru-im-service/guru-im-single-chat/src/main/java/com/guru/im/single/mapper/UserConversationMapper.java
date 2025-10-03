package com.guru.im.single.mapper;

import com.guru.im.single.model.pojo.UserConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserConversationMapper {
    int incrementUnreadCount(@Param("userId") Long userId,
                             @Param("conversationId") Long conversationId,
                             @Param("increment") Integer increment,
                             @Param("updateTime") Long updateTime);

}