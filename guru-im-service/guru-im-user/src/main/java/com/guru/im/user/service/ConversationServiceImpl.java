package com.guru.im.user.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.protocol.model.ConversationType;
import com.guru.im.user.mapper.ConversationMapper;
import com.guru.im.user.mapper.GroupMapper;
import com.guru.im.user.mapper.UserConversationMapper;
import com.guru.im.user.mapper.UserMapper;
import com.guru.im.user.model.pojo.Conversation;
import com.guru.im.user.model.pojo.Group;
import com.guru.im.user.model.pojo.User;
import com.guru.im.user.model.pojo.UserConversation;
import com.guru.im.user.model.vo.UserConversationVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {
    @Autowired
    private UserConversationMapper userConversationMapper;
    @Autowired
    private UserService userService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public List<UserConversationVO> getUserConversationsOld(Long userId) {
        // 1. 查询用户会话关系及基础会话信息
        List<UserConversation> userConversations =
                userConversationMapper.selectUserConversationWithBaseInfo(userId);

        if (CollectionUtils.isEmpty(userConversations)) {
            return Collections.emptyList();
        }

        // 2. 组装VO
        List<UserConversationVO> voList = new ArrayList<>();
        for (UserConversation uc : userConversations) {
            UserConversationVO vo = buildConversationVO(uc);
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 构建会话VO（方法1使用）
     */
    private UserConversationVO buildConversationVO(UserConversation uc) {
        UserConversationVO vo = new UserConversationVO();
        vo.setId(uc.getId());
        vo.setUserId(uc.getUserId());
        vo.setConversationId(uc.getConversationId());
        vo.setConversationType(uc.getConversationType());
        vo.setConversationKey(uc.getConversationKey());
        vo.setShowName(uc.getShowNickname());
        vo.setUnreadCount(uc.getUnreadCount());
        vo.setTop(uc.getTop());
        vo.setMute(uc.getMute());
        vo.setLastMessageContent(uc.getLastMessageContent());
        vo.setLastMessageTime(uc.getLastMessageTime());
        vo.setLastMessageSender(uc.getLastMessageSender());
        vo.setLastMessageSeq(uc.getLastMessageSeq());
        vo.setUpdateTime(uc.getUpdateTime());
        vo.setCreateTime(uc.getCreateTime());

        // 设置会话其他信息
        setConversationExt(vo, uc);

        return vo;
    }

    /**
     * 设置会话
     */
    private void setConversationExt(UserConversationVO vo, UserConversation uc) {
        if (uc.getConversationType() == 1) { // 单聊
            // 获取好友头像（这里需要根据showName或会话key解析出好友ID）
            Long friendId = extractFriendId(uc.getConversationType(), uc.getUserId(), uc.getConversationKey());
            User userInfo = userService.getUserById(friendId);

            vo.setAvatar(userInfo.getAvatar());
            vo.setShowName(userInfo.getUserName());
        } else if (uc.getConversationType() == 2) { // 群聊
            vo.setAvatar(uc.getConversationAvatar()); // 使用群头像
        }
    }

    @Override
    public Long extractFriendId(int conversationType, long userId, String conversationKey) {
        try {
            if (conversationType == ConversationType.PRIVATE_VALUE) {
                List<Long> userIds = Arrays.stream(conversationKey.split("_")).map(Long::parseLong)
                        .toList();
                if (userIds.get(0) == userId) {
                    return userIds.get(1);
                }
                return userIds.get(0);
            }else {
                String groupIdStr = conversationKey.substring(conversationKey.indexOf("_") + 1);
                return Long.parseLong(groupIdStr);
            }

        } catch (Exception e) {
            throw new RuntimeException("get friendId by conversionKey failed [" + conversationKey + "]:" + e.getMessage());
        }
    }

    @Override
    public ResponseResult<Conversation> createPrivateConversation(Long userId, Long friendId) {
        return null;
    }

    @Override
    @Transactional
    public ResponseResult<Conversation> createGroupConversation(Long groupId, String groupName,
                                                                String groupAvatar, List<Long> memberIds) {
        try {
            // 检查是否已存在会话
            String conversationKey = "group_" + groupId;
            Conversation existingConversation = conversationMapper.selectConversationByKey(conversationKey);
            if (existingConversation != null) {
                return ResponseResult.fail("会话已存在");
            }

            // 创建群聊会话
            Conversation conversation = new Conversation();
            conversation.setId(snowflakeIdGenerator.nextId());
            conversation.setConversationType(2); // 群聊
            conversation.setConversationKey(conversationKey);
            conversation.setConversationName(groupName);
            conversation.setConversationAvatar(groupAvatar);
            conversation.setStatus(1);
            conversation.setVersion(1L);
            long now = System.currentTimeMillis();
            conversation.setCreateTime(now);
            conversation.setUpdateTime(now);

            conversationMapper.insertConversation(conversation);

            // 为每个成员创建用户会话
            for (Long memberId : memberIds) {
                createUserConversation(memberId, conversation, groupName, now);
            }

            return ResponseResult.ok("群聊会话创建成功", conversation);

        } catch (Exception e) {
            throw new RuntimeException("创建群聊会话失败", e);
        }
    }

    @Override
    public ResponseResult<List<UserConversation>> getUserConversations(Long userId) {
        List<UserConversation> userConversations = userConversationMapper.selectUserConversations(userId);

        if (userConversations == null) {
            return ResponseResult.ok(new ArrayList<>());
        }
        for (UserConversation uc : userConversations) {
            // 填充会话基本信息
            Conversation conversation = conversationMapper.selectConversationById(uc.getConversationId());
            uc.setConversation(conversation);

            // 如果是群聊会话，填充群组信息
            if (uc.getConversationType() == 2) { // 群聊
                // 从conversation_key中提取群ID（格式：group_123）
                String conversationKey = conversation.getConversationKey();
                if (conversationKey.startsWith("group_")) {
                    Long groupId = Long.parseLong(conversationKey.substring(6));
                    Group group = groupMapper.selectGroupById(groupId);
                    if (group != null) {
                        uc.setGroupInfo(group);

                        // 如果会话名称为空，使用群名称
                        if (conversation.getConversationName() == null) {
                            conversation.setConversationName(group.getGroupName());
                        }
                        if (conversation.getConversationAvatar() == null) {
                            conversation.setConversationAvatar(group.getGroupAvatar());
                        }
                    }
                }
            }
        }

        return ResponseResult.ok(userConversations);
    }

    @Override
    @Transactional
    public ResponseResult<Boolean> updateConversationInfo(Long conversationId, String conversationName,
                                                          String conversationAvatar) {
        try {
            Conversation conversation = conversationMapper.selectConversationById(conversationId);
            if (conversation == null) {
                return ResponseResult.fail("会话不存在");
            }

            if (conversationName != null) {
                conversation.setConversationName(conversationName);
            }
            if (conversationAvatar != null) {
                conversation.setConversationAvatar(conversationAvatar);
            }
            conversation.setUpdateTime(System.currentTimeMillis());

            conversationMapper.updateConversation(conversation);
            return ResponseResult.ok("会话信息更新成功", true);

        } catch (Exception e) {
            throw new RuntimeException("更新会话信息失败", e);
        }
    }

    @Override
    public ResponseResult<Boolean> deleteUserConversation(Long userId, Long conversationId) {
        int update = userConversationMapper.deleteUserConversation(userId, conversationId);
        return ResponseResult.ok(update > 0);
    }

    @Override
    public ResponseResult<Boolean> updateConversationSetting(Long userId, Long conversationId, Boolean isTop, Boolean isMute) {
        UserConversation userConversation = userConversationMapper.selectUserConversation(userId, conversationId);
        if (userConversation != null) {
            int update = userConversationMapper.updateUserConversation(userConversation);
            return ResponseResult.ok(update > 0);
        }

        return ResponseResult.fail("用户会话信息不存在");
    }

    /**
     * 创建用户会话
     */
    private void createUserConversation(Long userId, Conversation conversation,
                                        String showNickname, long createTime) {
        UserConversation userConversation = new UserConversation();
        userConversation.setId(snowflakeIdGenerator.nextId());
        userConversation.setUserId(userId);
        userConversation.setConversationId(conversation.getId());
        userConversation.setConversationType(conversation.getConversationType());
        userConversation.setUnreadCount(0);
        userConversation.setLastReadSeq(0L);
        userConversation.setTop(false);
        userConversation.setMute(false);
        userConversation.setShowNickname(showNickname);
        userConversation.setStatus(1);
        userConversation.setCreateTime(createTime);
        userConversation.setUpdateTime(createTime);

        userConversationMapper.insertUserConversation(userConversation);
    }

    @Override
    public ResponseResult<UserConversation> getUserConversationDetail(Long userId, Long conversationId) {
        UserConversation userConversation = userConversationMapper.selectUserConversation(userId, conversationId);
        if (userConversation == null) {
            return ResponseResult.fail("用户会话不存在");
        }

        // 填充会话信息
        Conversation conversation = conversationMapper.selectConversationById(conversationId);
        userConversation.setConversation(conversation);

        // 如果是群聊，填充群组信息
        if (userConversation.getConversationType() == 2) {
            String conversationKey = conversation.getConversationKey();
            if (conversationKey.startsWith("group_")) {
                Long groupId = Long.parseLong(conversationKey.substring(6));
                Group group = groupMapper.selectGroupById(groupId);
                userConversation.setGroupInfo(group);
            }
        }

        return ResponseResult.ok(userConversation);
    }

    @Override
    public Conversation selectConversationByKey(String conversationKey) {
        return conversationMapper.selectConversationByKey(conversationKey);
    }
}
