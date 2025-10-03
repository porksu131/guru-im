package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.ConversationLatestSeq;
import com.guru.im.offline.model.pojo.ConversationUnreadCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConversationMapper {
    /**
     * 查询用户各会话的未读数量 - 返回List
     */
    List<ConversationUnreadCount> findUnreadCountsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户会话的最新序列号 - 返回List
     */
    List<ConversationLatestSeq> findLatestSeqsByUserId(@Param("userId") Long userId);
}