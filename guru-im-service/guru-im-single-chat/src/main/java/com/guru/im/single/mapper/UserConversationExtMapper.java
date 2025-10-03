package com.guru.im.single.mapper;

import com.guru.im.single.model.pojo.UserConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserConversationExtMapper {
    
    List<UserConversation> selectWithConversation(@Param("userId") Long userId,
                                                  @Param("offset") Integer offset,
                                                  @Param("limit") Integer limit);
    
    UserConversation selectDetailByUserConversation(@Param("userId") Long userId,
                                                  @Param("conversationId") Long conversationId);
    
    List<UserConversation> selectTopConversationsWithDetail(@Param("userId") Long userId,
                                                          @Param("limit") Integer limit);
}