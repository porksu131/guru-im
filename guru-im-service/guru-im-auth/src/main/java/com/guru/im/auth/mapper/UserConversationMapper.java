package com.guru.im.auth.mapper;

import com.guru.im.auth.model.pojo.UserConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserConversationMapper {
    int insert(UserConversation userConversation);
}