package com.guru.im.user.rocketmq.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.cache.starter.distribute.id.SequenceIdGenerator;
import com.guru.im.common.constant.*;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.user.mapper.GroupInviteNotifyMapper;
import com.guru.im.user.model.pojo.Group;
import com.guru.im.user.model.pojo.GroupInvitePojo;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupInviteNotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupInviteNotifyService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private GroupInviteNotifyMapper groupInviteNotifyMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private SequenceIdGenerator sequenceIdGenerator;
    @Autowired
    private MQMessageSender mqMessageSender;

    public void pushGroupInviteNotify(Group group, List<Long> allMemberIds) {
        // 创建群聊邀请通知
        GroupInvitePojo notifyEntity = new GroupInvitePojo();
        notifyEntity.setId(snowflakeIdGenerator.nextId());
        notifyEntity.setGroupId(group.getId());
        notifyEntity.setInviterId(group.getGroupOwner());
        notifyEntity.setInviteReason(group.getGroupIntro());
        notifyEntity.setDeliveryStatus(DeliveryStatus.WAIT_DELIVERY.getCode());
        notifyEntity.setExpireTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000); // 7天后过期
        notifyEntity.setCreateTime(System.currentTimeMillis());
        notifyEntity.setUpdateTime(System.currentTimeMillis());
        notifyEntity.setGlobalSeq(sequenceIdGenerator.nextGlobalSeq());
        String membersJson = "";
        try {
            membersJson = objectMapper.writeValueAsString(notifyEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        notifyEntity.setInitialMembersJson(membersJson);

        // 保存群聊邀请通知
        groupInviteNotifyMapper.insert(notifyEntity);

        GroupInviteNotify.Builder builder = GroupInviteNotify.newBuilder();
        builder.setGroupId(group.getId());
        builder.setGroupName(group.getGroupName());
        builder.setInviterId(group.getGroupOwner());
        builder.setInviterName(group.getOwnerInfo().getUserName());
        builder.setInviteReason(group.getGroupIntro());
        builder.setRequestStatus(RequestStatus.ACCEPTED);
        builder.addAllInitialMembers(allMemberIds);
        builder.setId(notifyEntity.getId());
        builder.setGlobalSeq(notifyEntity.getGlobalSeq());
        builder.setCreateTime(notifyEntity.getCreateTime());
        if (group.getGroupAvatar() != null) {
            builder.setGroupAvatar(group.getGroupAvatar());
        }
        GroupInviteNotify notify = builder.build();


        // 推送群聊邀请通知
        MQMessageWrapper wrapper = buildMQWrapper(notify, allMemberIds, notifyEntity.getId());
        mqMessageSender.sendAsync(wrapper, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                LOGGER.info("推送群邀请通知到MQ, ID[{}]，成功", notifyEntity.getId());
            }

            @Override
            public void onException(Throwable throwable) {
                LOGGER.error("推送群邀请通知到MQ，ID[{}]，失败：{}", notifyEntity.getId(), throwable.getMessage());
            }
        });
    }

    private MQMessageWrapper buildMQWrapper(GroupInviteNotify groupInviteNotify, List<Long> receiverIds, Long correlationId) {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.GROUP_INVITE)
                .setGroupInvite(groupInviteNotify)
                .build();
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setGlobalSeq(groupInviteNotify.getGlobalSeq());
        wrapper.setSourceType(SourceType.USER_SERVICE);
        wrapper.setMessageType(MQMessageType.ACTION);
        wrapper.setTargetTopic(MQTopic.DISPATCH_ACTION_TOPIC);
        wrapper.setReplyTopic(MQTopic.USR_TOPIC);
        wrapper.setReplyTag(MQTag.ACK);
        wrapper.setCorrelationId(String.valueOf(correlationId));
        wrapper.setCorrelationType(CorrelationType.GROUP_INVITE_NOTIFY);
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setReceiverIds(receiverIds);
        wrapper.setBody(imMessage.toByteArray());
        return wrapper;
    }


    // 处理ack响应
    public void processGroupInviteAck(String correlationId, Response response) {
        Long id = Long.parseLong(correlationId);
        DeliveryStatus deliveryStatus = ResponseCode.SUCCESS == response.getCode() ?
                DeliveryStatus.ARRIVED : DeliveryStatus.ARRIVE_FAILED;
        int result = groupInviteNotifyMapper.updateDeliveryStatus(id, deliveryStatus.getCode(), System.currentTimeMillis());
        if (result <= 0) {
            LOGGER.warn("update group invite notify failed, id{} may be not exist", id);
        }
    }
}