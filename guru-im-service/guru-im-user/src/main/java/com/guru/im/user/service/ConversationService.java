package com.guru.im.user.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.Conversation;
import com.guru.im.user.model.pojo.UserConversation;
import com.guru.im.user.model.vo.UserConversationVO;

import java.util.List;

public interface ConversationService {
    /**
     * 获取用户会话列表
     */
    List<UserConversationVO> getUserConversationsOld(Long userId);

    /**
     * 根据会话key获取对方ID（群组或好友ID）
     */
    Long extractFriendId(int conversationType, long userId, String conversationKey);

    /**
     * 创建私聊会话
     */
    ResponseResult<Conversation> createPrivateConversation(Long userId, Long friendId);

    /**
     * 创建群聊会话
     */
    ResponseResult<Conversation> createGroupConversation(Long groupId, String groupName,
                                                         String groupAvatar, List<Long> memberIds);

    /**
     * 获取用户会话列表
     */
    ResponseResult<List<UserConversation>> getUserConversations(Long userId);

    /**
     * 更新会话信息
     */
    ResponseResult<Boolean> updateConversationInfo(Long conversationId, String conversationName,
                                                   String conversationAvatar);

    /**
     * 删除用户会话（软删除）
     */
    ResponseResult<Boolean> deleteUserConversation(Long userId, Long conversationId);

    /**
     * 设置会话置顶/免打扰
     */
    ResponseResult<Boolean> updateConversationSetting(Long userId, Long conversationId,
                                                      Boolean isTop, Boolean isMute);

    /**
     * 根据会话key查询会话信息
     */
    Conversation selectConversationByKey(String conversationKey);

    /**
     * 获取用户会话详细信息
     */
    ResponseResult<UserConversation> getUserConversationDetail(Long userId, Long conversationId);
}