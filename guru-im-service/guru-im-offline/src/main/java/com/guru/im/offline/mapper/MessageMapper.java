package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    List<Message> findRecentMessages(@Param("conversationType") Integer conversationType,
                                     @Param("conversationId") Long conversationId,
                                     @Param("maxSeq") Long maxSeq,
                                     @Param("limit") int limit);

    Integer countMessages(@Param("conversationType") Integer conversationType,
                          @Param("conversationId") Long conversationId,
                          @Param("maxSeq") Long maxSeq);

    Long findMaxServerSeq(@Param("conversationType") Integer conversationType,
                          @Param("conversationId") Long conversationId);
}