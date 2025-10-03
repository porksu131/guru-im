package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.UserConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserConversationMapper {

    int insert(UserConversation userConversation);

    int update(UserConversation userConversation);

    int batchInsert(@Param("userConversations") List<UserConversation> userConversations);

    int updateUnreadCount(@Param("id") Long id,
                          @Param("unreadCount") Integer unreadCount);

    int incrementUnreadCount(@Param("userId") Long userId,
                             @Param("conversationId") Long conversationId,
                             @Param("increment") Integer increment,
                             @Param("updateTime") Long updateTime);

    int markAsRead(@Param("userId") Long userId,
                   @Param("conversationId") Long conversationId,
                   @Param("lastReadSeq") Long lastReadSeq);

    int setConversationTop(@Param("userId") Long userId,
                           @Param("conversationId") Long conversationId,
                           @Param("isTop") Boolean isTop);

    int setConversationMute(@Param("userId") Long userId,
                            @Param("conversationId") Long conversationId,
                            @Param("isMute") Boolean isMute);

    int updateShowNickname(@Param("userId") Long userId,
                           @Param("conversationId") Long conversationId,
                           @Param("showNickname") String showNickname);

    int deleteByUserConversation(@Param("userId") Long userId,
                                 @Param("conversationId") Long conversationId);

    int deleteByUserId(@Param("userId") Long userId);

    UserConversation selectById(@Param("id") Long id);

    UserConversation selectByUserConversation(@Param("userId") Long userId,
                                              @Param("conversationId") Long conversationId);

    List<UserConversation> selectByUserId(@Param("userId") Long userId,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    List<UserConversation> selectTopConversations(@Param("userId") Long userId,
                                                  @Param("limit") Integer limit);

    List<UserConversation> selectWithUnreadMessages(@Param("userId") Long userId,
                                                    @Param("minUnreadCount") Integer minUnreadCount);

    int countByUserId(@Param("userId") Long userId);

    int sumUnreadCount(@Param("userId") Long userId);

    List<Long> findConversationIdsByUser(@Param("userId") Long userId);



    /**
     * 根据用户ID查询会话关系列表（包含基础会话信息）
     */
    List<UserConversation> selectUserConversationWithBaseInfo(@Param("userId") Long userId);

    /**
     * 批量查询用户会话关系
     */
    List<UserConversation> selectListByUserAndConversationIds(@Param("userId") Long userId,
                                                                @Param("conversationIds") List<Long> conversationIds);


    // 用户会话相关
    int insertUserConversation(UserConversation userConversation);
    int updateUserConversation(UserConversation userConversation);
    UserConversation selectUserConversation(@Param("userId") Long userId,
                                            @Param("conversationId") Long conversationId);
    List<UserConversation> selectUserConversations(@Param("userId") Long userId);
    int deleteUserConversation(@Param("userId") Long userId,
                               @Param("conversationId") Long conversationId);
}