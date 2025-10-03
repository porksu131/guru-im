package com.guru.im.user.rocketmq.handler;

import com.guru.im.common.constant.*;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.user.mapper.FriendRequestMapper;
import com.guru.im.user.model.pojo.FriendRequest;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendNotifyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendNotifyService.class);
    @Autowired
    private MQMessageSender mqMessageSender;

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    public void pushFriendNotify(FriendRequest friendRequest, NotifyType notifyType) {
        FriendRequestNotify.Builder builder = FriendRequestNotify.newBuilder();
        builder.setId(friendRequest.getId());
        builder.setGlobalSeq(friendRequest.getGlobalSeq());
        builder.setRequesterId(friendRequest.getRequesterId());
        builder.setRequesterName(friendRequest.getRequesterName());
        builder.setRequestMsg(friendRequest.getRequestMsg());
        builder.setRequestStatus(RequestStatus.forNumber(friendRequest.getRequestStatus()));
        builder.setCreateTime(friendRequest.getCreateTime());
        builder.setRequestType(FriendRequestNotify.RequestType.forNumber(friendRequest.getRequestType()));
        builder.setResponderId(friendRequest.getResponderId());
        builder.setResponderName(friendRequest.getResponderName());

        if (notifyType == NotifyType.ADD_FRIEND_RESPONSE) {
            // 如果是同意或拒绝的操作
            builder.setResponseTime(friendRequest.getUpdateTime());
        }

        FriendRequestNotify friendRequestNotify = builder.build();

        // 通知双方
        List<Long> receiverIds = new ArrayList<>();
        receiverIds.add(friendRequest.getResponderId());
        receiverIds.add(friendRequest.getRequesterId());

        MQMessageWrapper wrapper = buildMQWrapper(friendRequestNotify, String.valueOf(friendRequest.getId()), receiverIds);
        mqMessageSender.sendAsync(wrapper.getDestination(), wrapper, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                LOGGER.debug("已推送好友关系变更通知到MQ, ID[{}]", friendRequest.getId());
            }

            @Override
            public void onException(Throwable throwable) {
                LOGGER.error("推送好友关系变更通知到MQ，ID[{}]，失败：{}", friendRequest.getId(), throwable.getMessage());
            }
        });
    }


    private MQMessageWrapper buildMQWrapper(FriendRequestNotify friendRequestNotify, String correlationId, List<Long> receiverIds) {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.FRIEND_REQUEST)
                .setFriendRequest(friendRequestNotify)
                .build();
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setGlobalSeq(friendRequestNotify.getGlobalSeq());
        wrapper.setSourceType(SourceType.USER_SERVICE);
        wrapper.setMessageType(MQMessageType.ACTION);
        wrapper.setTargetTopic(MQTopic.DISPATCH_ACTION_TOPIC);
        wrapper.setReplyTopic(MQTopic.USR_TOPIC);
        wrapper.setReplyTag(MQTag.ACK);
        wrapper.setCorrelationId(String.valueOf(correlationId));
        wrapper.setCorrelationType(CorrelationType.FRIEND_REQUEST_NOTIFY);
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setReceiverIds(receiverIds);
        wrapper.setBody(imMessage.toByteArray());
        return wrapper;
    }


    public void processFriendRelationChange(String friendRequestId, Response response) {
        FriendRequest friendRequest = friendRequestMapper.findById(Long.parseLong(friendRequestId));
        if (friendRequest != null) {
            friendRequest.setNotifyStatus(ResponseCode.SUCCESS == response.getCode() ? 1 : 2); // 0：待通知，1：已通知， 2：通知失败
            friendRequest.setUpdateTime(System.currentTimeMillis());
            friendRequestMapper.updateRequestStatus(friendRequest);
        }
    }
}
