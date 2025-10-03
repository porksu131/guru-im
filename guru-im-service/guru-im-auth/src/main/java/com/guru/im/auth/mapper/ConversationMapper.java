package com.guru.im.auth.mapper;

import com.guru.im.auth.model.pojo.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper {

    int insert(Conversation conversation);
}