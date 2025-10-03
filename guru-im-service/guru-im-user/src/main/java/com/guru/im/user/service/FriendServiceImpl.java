package com.guru.im.user.service;

import com.guru.im.cache.starter.distribute.id.SequenceIdGenerator;
import com.guru.im.common.constant.NotifyType;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.protocol.model.ConversationType;
import com.guru.im.user.mapper.*;
import com.guru.im.user.model.pojo.*;
import com.guru.im.user.model.vo.FriendRelationVO;
import com.guru.im.user.model.vo.FriendRequestVO;
import com.guru.im.user.model.vo.FriendStatusVO;
import com.guru.im.user.model.vo.UserConversationVO;
import com.guru.im.user.rocketmq.handler.FriendNotifyService;
import com.guru.im.user.rocketmq.handler.UserConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private SequenceIdGenerator sequenceIdGenerator;
    @Autowired
    private UserConnectionService userConnectionService;
    @Autowired
    private FriendNotifyService friendNotifyService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private UserConversationMapper userConversationMapper;

    @Override
    public ResponseResult<List<FriendRelationVO>> getFriendsByUserId(long uid) {
        List<FriendRelationVO> friends = getFriendsWithStatus(uid);
        List<UserConversationVO> userConversations = conversationService.getUserConversationsOld(uid);
        if (!userConversations.isEmpty()) {
            friends.forEach(friend -> {
                UserConversationVO userConversationVO = userConversations.stream()
                        .filter(obj -> {
                            long friendId = conversationService.extractFriendId(obj.getConversationType(), uid, obj.getConversationKey());
                            return friendId == friend.getFriendId();
                        })
                        .findFirst().orElse(null);
                friend.setUserConversation(userConversationVO);
            });
        }
        return ResponseResult.ok(friends);
    }

    @Transactional
    @Override
    public ResponseResult<Void> addFriend(long uid, long friendId) {
        if (friendRelationMapper.checkFriendExists(uid, friendId) > 0) {
            return ResponseResult.fail("已是好友关系");
        }

        User user = userMapper.getByUid(uid);
        User friend = userMapper.getByUid(friendId);
        if (user == null || friend == null) {
            return ResponseResult.fail("用户不存在");
        }
        // 简单实现，无需好友确认，默认双方互相添加好友，所以添加两份好友关系
        FriendRelation relation0 = buildFriendRelation(user, friend);
        friendRelationMapper.save(relation0);

        // 简单实现，无需好友确认，默认双方互相添加好友
        FriendRelation relation1 = buildFriendRelation(friend, user);
        friendRelationMapper.save(relation1);
        return ResponseResult.ok();
    }

    private FriendRelation buildFriendRelation(User user, User friend) {
        FriendRelation relation = new FriendRelation();
        relation.setId(snowflakeIdGenerator.nextId());
        relation.setUid(user.getUid());
        relation.setUserName(user.getUserName());
        relation.setFriendId(friend.getUid());
        relation.setFriendName(friend.getUserName());
        relation.setRelationStatus(1); // 1:正常 2:拉黑 3:删除
        relation.setCreateTime(System.currentTimeMillis());
        relation.setUpdateTime(System.currentTimeMillis());
        return relation;
    }

    // 获取好友列表（带在线状态）
    public List<FriendRelationVO> getFriendsWithStatus(Long userId) {
        // 1. 查询好友关系
        List<FriendRelation> relations = friendRelationMapper.findFriendsByUid(userId);

        // 2. 提取好友ID
        List<Long> friendIds = relations.stream()
                .map(FriendRelation::getFriendId)
                .collect(Collectors.toList());

        if (friendIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量获取好友状态
        Map<Long, FriendStatusVO> statusMap = userConnectionService.batchGetStatus(friendIds);

        // 4. 组合结果
        return relations.stream().map(relation -> {
            FriendRelationVO vo = new FriendRelationVO();
            vo.setId(relation.getId());
            vo.setUid(relation.getUid());
            vo.setFriendId(relation.getFriendId());
            vo.setFriendName(relation.getFriendName());
            vo.setRelationStatus(relation.getRelationStatus());
            vo.setCreateTime(relation.getCreateTime());
            vo.setUpdateTime(relation.getUpdateTime());

            FriendStatusVO status = statusMap.get(relation.getFriendId());
            if (status != null) {
                vo.setOnlineStatus(status.getOnlineStatus());
                vo.setLastActiveTime(status.getLastActiveTime());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ResponseResult<FriendRequest> sendFriendRequest(FriendRequestVO request) {
        if (1 == request.getRequestType()) {
            // 添加好友
            return addFriendRequest(request);
        }
        // 拉黑好友，解除拉黑，删除好友，操作上基本一样
        return changeFriendRelation(request);
    }

    public ResponseResult<FriendRequest> addFriendRequest(FriendRequestVO request) {
        // 验证是否已是好友
        FriendRelation friendRelation = friendRelationMapper.findByUidFriendId(request.getRequesterId(), request.getResponderId());
        if (friendRelation != null) {
            if (friendRelation.getRelationStatus() == 1) {
                return ResponseResult.fail("已是好友关系");
            }
            if (friendRelation.getRelationStatus() == 2) {
                return ResponseResult.fail("已被拉黑，无法添加");
            }
        }

        // 验证是否已存在记录
        if (friendRequestMapper.checkRequestExists(request.getRequesterId(), request.getResponderId()) > 0) {
            return ResponseResult.fail("已存在待处理的好友请求");
        }

        User user = userMapper.getByUid(request.getRequesterId());
        if (user == null) {
            return ResponseResult.fail("用户[" + request.getRequesterId() + "]不存在");
        }

        User responder = userMapper.getByUid(request.getResponderId());
        if (responder == null) {
            return ResponseResult.fail("用户[" + request.getResponderId() + "]不存在");
        }

        long now = System.currentTimeMillis();
        // 创建好友请求记录
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(snowflakeIdGenerator.nextId());
        friendRequest.setGlobalSeq(sequenceIdGenerator.nextGlobalSeq());
        friendRequest.setRequesterId(request.getRequesterId());
        friendRequest.setRequesterName(user.getUserName());
        friendRequest.setResponderName(responder.getUserName());
        friendRequest.setResponderId(request.getResponderId());
        friendRequest.setRequestMsg(request.getRequestMsg());
        friendRequest.setRequestType(request.getRequestType());
        friendRequest.setRequestStatus(0); // 0:待处理 1:已同意 2:已拒绝
        friendRequest.setNotifyStatus(0); // 0:待通知 1：已通知
        friendRequest.setCreateTime(now);
        friendRequest.setUpdateTime(now);
        friendRequestMapper.save(friendRequest);

        // 通知接收方
        friendNotifyService.pushFriendNotify(friendRequest, NotifyType.ADD_FRIEND_REQUEST);
        return ResponseResult.ok(friendRequest);
    }

    public ResponseResult<FriendRequest> changeFriendRelation(FriendRequestVO request) {
        Long uid = request.getRequesterId();
        Long friendId = request.getResponderId();

        FriendRelation userRelation = friendRelationMapper.findByUidFriendId(uid, friendId);
        FriendRelation friendRelation = friendRelationMapper.findByUidFriendId(friendId, uid);
        if (friendRelation == null || userRelation == null) {
            return ResponseResult.fail("无好友关系");
        }

        // 验证是否已存在记录
        if (friendRequestMapper.checkRequestExists(uid, friendId) > 0) {
            return ResponseResult.fail("已存在待处理的好友关系变更请求");
        }

        long now = System.currentTimeMillis();
        // 创建好友变更请求记录
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(snowflakeIdGenerator.nextId());
        friendRequest.setGlobalSeq(sequenceIdGenerator.nextGlobalSeq());
        friendRequest.setRequesterId(uid);
        friendRequest.setResponderId(friendId);
        friendRequest.setRequestMsg(null);
        friendRequest.setRequestStatus(1); // 0:待处理 1:已同意 2:已拒绝， 拉黑、删除操作，默认已同意，即需对方同意
        friendRequest.setNotifyStatus(0); // 0:待通知 1:已通知
        friendRequest.setRequestType(request.getRequestType());
        friendRequest.setCreateTime(now);
        friendRequest.setUpdateTime(now);
        friendRequestMapper.save(friendRequest);

        if (3 == request.getRequestType()) {
            // 删除好友关系
            friendRelationMapper.deleteByUidFriendId(uid, friendId);
            friendRelationMapper.deleteByUidFriendId(friendId, uid);
        } else {
            // 更新好友关系状态，拉黑或解除来黑
            friendRelation.setRelationStatus(request.getRequestType() == 2 ? 2 : 1); // 1:正常 2:拉黑
            friendRelation.setUpdateTime(System.currentTimeMillis());
            friendRelationMapper.updateRelationStatus(friendRelation);
        }

        // 好友关系变更，通知对方
        friendNotifyService.pushFriendNotify(friendRequest, NotifyType.FRIEND_RELATION_CHANGE);
        return ResponseResult.ok(friendRequest);
    }

    @Override
    public ResponseResult<Void> handleFriendRequest(FriendRequestVO request) {
        if (1 != request.getRequestType()) {
            return ResponseResult.fail("无效的请求类型");
        }
        if (request.getRequestStatus() != 1 & request.getRequestStatus() != 2) {
            return ResponseResult.fail("无效的请求状态");
        }
        // 查询并验证请求
        FriendRequest friendRequest = friendRequestMapper.findById(request.getId());
        if (friendRequest == null) {
            return ResponseResult.fail("无效的好友变更");
        }

        // 更新请求状态
        friendRequest.setGlobalSeq(sequenceIdGenerator.nextGlobalSeq());
        friendRequest.setRequestStatus(request.getRequestStatus()); // 请求状态：0：待处理，1-同意，2-拒绝
        friendRequest.setNotifyStatus(0); // 通知状态：0-待通知，1-已通知
        friendRequest.setUpdateTime(System.currentTimeMillis());
        friendRequestMapper.updateRequestStatus(friendRequest);


        // 如果同意，建立好友关系，建立好友会话
        if (request.getRequestStatus() == 1) {
            User requester = userMapper.getByUid(friendRequest.getRequesterId());
            User receiver = userMapper.getByUid(friendRequest.getResponderId());
            if (requester == null || receiver == null) {
                return ResponseResult.fail("用户不存在");
            }
            // 好友关系
            createFriendRelation(requester, receiver);
            createFriendRelation(receiver, requester);

            // 好友会话
            Long conversationId = snowflakeIdGenerator.nextId();
            createConversation(conversationId, requester, receiver);
            createUserConversation(conversationId, requester, receiver);

        }

        // 不管同意或拒绝，都通知请求方
        friendNotifyService.pushFriendNotify(friendRequest, NotifyType.ADD_FRIEND_RESPONSE);

        return ResponseResult.ok();
    }

    private void createFriendRelation(User user, User friend) {
        FriendRelation friendRelation = buildFriendRelation(user, friend);
        friendRelationMapper.save(friendRelation);
    }


    private void createConversation(Long conversationId, User sender, User receiver) {
        long now = System.currentTimeMillis();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setConversationType(1); // 1:私聊
        conversation.setConversationKey(buildConversationKey(sender.getUid(), receiver.getUid()));
        conversation.setConversationName(sender.getUserName() + "-" + receiver.getUserName());
        conversation.setStatus(1);
        conversation.setVersion(1L);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insert(conversation);
    }

    private void createUserConversation(Long conversationId, User sender, User receiver) {
        UserConversation userConvA = buildUserConversation(conversationId, sender, receiver);
        UserConversation userConvB = buildUserConversation(conversationId, receiver, sender);
        userConversationMapper.insert(userConvA);
        userConversationMapper.insert(userConvB);
    }

    private String buildConversationKey(Long senderId, Long receiverId) {
        return Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);
    }

    private UserConversation buildUserConversation(Long conversationId, User sender, User receiver) {
        long now = System.currentTimeMillis();
        UserConversation userConversation = new UserConversation();
        userConversation.setId(snowflakeIdGenerator.nextId());
        userConversation.setUserId(sender.getUid());
        userConversation.setConversationId(conversationId);
        userConversation.setConversationType(1);
        userConversation.setShowNickname(receiver.getUserName());
        userConversation.setCreateTime(now);
        userConversation.setUpdateTime(now);
        userConversation.setStatus(1);
        userConversation.setUnreadCount(0); // 接收方未读数为0
        userConversation.setLastReadSeq(0L); // 接收方还未阅读
        userConversation.setTop(false);
        userConversation.setMute(false);
        return userConversation;
    }
}
