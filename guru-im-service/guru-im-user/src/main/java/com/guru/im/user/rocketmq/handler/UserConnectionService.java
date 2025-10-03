package com.guru.im.user.rocketmq.handler;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.model.PresenceNotify;
import com.guru.im.protocol.model.UserPresence;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.user.mapper.FriendRelationMapper;
import com.guru.im.user.model.pojo.FriendRelation;
import com.guru.im.user.model.vo.FriendStatusVO;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserConnectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserConnectionService.class);
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MQMessageSender mqMessageSender;

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private UserSessionManager userSessionManager;

    // 批量获取用户状态和活跃时间
    public Map<Long, FriendStatusVO> batchGetStatus(List<Long> userIds) {
        Map<Long, List<DeviceStatus>> longListMap = userSessionManager.batchGetUsersOnlineDevices(userIds);

        if (longListMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, FriendStatusVO> result = new HashMap<>();
        longListMap.forEach((k, v) -> {
            Integer onlineStatus = !v.isEmpty() ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE;
            FriendStatusVO FriendStatusVO = new FriendStatusVO();
            FriendStatusVO.setOnlineStatus(onlineStatus);
            FriendStatusVO.setLastActiveTime(!v.isEmpty() ? v.get(0).getLastActiveTime() : null);

            result.put(k, FriendStatusVO);
        });

        return result;
    }

    // 当用户在线状态变更，推送其状态给所有朋友
    public void processMQMessage(PresenceNotify presenceNotify) {
        long userId = presenceNotify.getUserId();
        String userStatus = getStatusText(presenceNotify.getStatus());
        LOGGER.debug("收到用户[{}]{}通知，开始推送给其所有好友", userId, userStatus);
        // 1. 查询好友关系
        List<FriendRelation> relations = friendRelationMapper.findFriendsByUid(userId);
        if (relations == null || relations.isEmpty()) {
            return;
        }

        // 2. 提取好友ID
        List<Long> friendIds = relations.stream()
                .map(FriendRelation::getFriendId)
                .toList();

        // 3. 消息推送，推送给所有好友
        MQMessageWrapper wrapper = buildMQWrapper(presenceNotify, friendIds);

        mqMessageSender.sendAsync(wrapper.getDestination(),
                wrapper, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        LOGGER.info("已推送用户[{}][{}]消息给其所有好友", userId, userStatus);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        LOGGER.warn("推送用户[{}][{}]消息给其所有好友失败，原因：{}",
                                userId, userStatus, throwable.getMessage());
                    }
                });
    }

    private MQMessageWrapper buildMQWrapper(PresenceNotify presenceNotify, List<Long> receiverIds) {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.ONEWAY)
                .setMessageType(MessageType.PRESENCE)
                .setPresenceNotify(presenceNotify)
                .build();
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setSourceType(SourceType.USER_SERVICE);
        wrapper.setMessageType(MQMessageType.ACTION);
        wrapper.setTargetTopic(MQTopic.DISPATCH_ACTION_TOPIC);
        wrapper.setCorrelationId(String.valueOf(presenceNotify.getUserId()));
        wrapper.setCorrelationType(CorrelationType.PRESENCE_NOTIFY);
        wrapper.setQos(MQQos.QOS_AT_MOST_ONCE);
        wrapper.setReceiverIds(receiverIds);
        wrapper.setBody(imMessage.toByteArray());
        return wrapper;
    }

    private String getStatusText(UserPresence userPresence) {
        if (userPresence == UserPresence.ONLINE) {
            return "在线";
        } else if (userPresence == UserPresence.OFFLINE) {
            return "离线";
        } else if (userPresence == UserPresence.BUSY) {
            return "忙碌";
        } else if (userPresence == UserPresence.INVISIBLE) {
            return "隐身";
        } else if (userPresence == UserPresence.AWAY) {
            return "离开";
        }

        return "";
    }
}
