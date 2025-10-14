package com.guru.im.signal.service;

import com.guru.im.common.constant.*;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.signal.model.pojo.CallParticipant;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageSenderImpl implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSenderImpl.class);
    @Autowired
    private MQMessageSender mqMessageSender;
    
    @Autowired
    private CallSessionService callSessionService;
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public void sendToUser(Long userId, SignalingMessage signaling) {
        
        // 获取用户所有在线设备
        List<String> onlineDevices = getUserOnlineDevices(userId);
        
        if (onlineDevices.isEmpty()) {
            log.warn("用户没有在线设备: userId={}", userId);
            return;
        }
        
        // 发送给所有在线设备
        for (String deviceId : onlineDevices) {
            try {
                sendToUserDevice(userId, deviceId, signaling);
            } catch (Exception e) {
                log.error("发送消息给用户设备失败: userId={}, deviceId={}", userId, deviceId, e);
            }
        }
        
        log.debug("发送消息给用户所有设备: userId={}, deviceCount={}, msgId={}, type={}", 
            userId, onlineDevices.size(), signaling.getMessageId(), signaling.getType());
    }

    @Override
    public void sendToUserDevice(Long userId, String deviceId, SignalingMessage signaling) {
        try {
            // 构建消息
            ImMessage imMessage = buildImMessage(signaling);
            // 发送到分发层
            sendToDispatch(imMessage, Collections.singletonList(userId), deviceId);
        } catch (Exception e) {
            log.error("发送消息给用户设备失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息到指定设备（新增）
     */
    public void sendToDevice(Long userId, String deviceId, SignalingMessage signaling) {
        try {
            // 构建设备目标列表
            List<DeviceTarget> deviceTargets = Collections.singletonList(
                    DeviceTarget.newBuilder()
                            .setUserId(userId)
                            .setDeviceId(deviceId)
                            .build()
            );

            // 构建包含设备目标的消息
            SignalingMessage deviceMessage = SignalingMessage.newBuilder(signaling)
                    .clearToDevices()
                    .addAllToDevices(deviceTargets)
                    .build();

            sendToDispatch(buildImMessage(deviceMessage), Collections.singletonList(userId), deviceId);

        } catch (Exception e) {
            log.error("发送消息到设备失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("发送消息到设备失败");
        }
    }

    /**
     * 发送消息到多个设备（新增）
     */
    public void sendToDevices(List<DeviceTarget> deviceTargets, SignalingMessage signaling) {
        if (deviceTargets == null || deviceTargets.isEmpty()) {
            log.warn("设备目标列表为空，跳过发送");
            return;
        }

        // 按用户分组设备
        Map<Long, List<String>> userDevices = new HashMap<>();
        for (DeviceTarget target : deviceTargets) {
            userDevices.computeIfAbsent(target.getUserId(), k -> new ArrayList<>())
                    .add(target.getDeviceId());
        }

        // 构建包含设备目标的消息
        SignalingMessage deviceMessage = SignalingMessage.newBuilder(signaling)
                .clearToDevices()
                .addAllToDevices(deviceTargets)
                .build();

        // 发送给每个设备的用户
        for (Map.Entry<Long, List<String>> entry : userDevices.entrySet()) {
            Long userId = entry.getKey();
            List<String> deviceIds = entry.getValue();

            for (String deviceId : deviceIds) {
                try {
                    sendToDispatch(buildImMessage(deviceMessage), Collections.singletonList(userId), deviceId);
                } catch (Exception e) {
                    log.error("发送消息到设备失败: userId={}, deviceId={}", userId, deviceId, e);
                }
            }
        }
    }

    /**
     * 构建ImMessage
     */
    private ImMessage buildImMessage(SignalingMessage signaling) {
        return com.guru.im.protocol.util.MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.ONEWAY)
                .setMessageType(MessageType.SIGNALING)
                .setSignalingMessage(signaling)
                .build();
    }

    private void sendToDispatch(ImMessage imMessage, List<Long> receiverIds, String deviceId) {
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setCorrelationType(CorrelationType.SIGNAL_MESSAGE);
        wrapper.setCorrelationId(String.valueOf(imMessage.getSignalingMessage().getMessageId()));
        wrapper.setSourceType(SourceType.SIGNAL_SERVICE);
        wrapper.setMessageType(MQMessageType.SIGNAL);
        // wrapper.setReplyTopic(MQTopic.SIGNAL_TOPIC); // todo
        // wrapper.setReplyTag(MQTag.ACK);
        wrapper.setTargetTopic(MQTopic.DISPATCH_SIGNAL_TOPIC);
        wrapper.setReceiverIds(receiverIds);
        wrapper.setDeviceId(deviceId);
        wrapper.setQos(MQQos.QOS_AT_MOST_ONCE); // 暂时todo
        wrapper.setBody(imMessage.toByteArray());
        mqMessageSender.sendAsync(wrapper, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("推送消息到MQ success");
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("推送消息到MQ failed:{}", throwable.getMessage());
            }
        });
    }



    @Override
    public void sendToUsers(List<Long> userIds, SignalingMessage signaling) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户列表为空，跳过发送");
            return;
        }
        
        for (Long userId : userIds) {
            try {
                sendToUser(userId, signaling);
            } catch (Exception e) {
                log.error("发送消息给用户失败: userId={}", userId, e);
                // 继续发送给其他用户，不中断
            }
        }
        
        log.debug("批量发送消息完成: userCount={}, msgId={}, type={}", 
            userIds.size(), signaling.getMessageId(), signaling.getType());
    }

    @Override
    public void sendToUserDevices(Map<Long, List<String>> userDevices, SignalingMessage signaling) {
        if (userDevices == null || userDevices.isEmpty()) {
            log.warn("用户设备映射为空，跳过发送");
            return;
        }
        
        int totalDevices = 0;
        for (Map.Entry<Long, List<String>> entry : userDevices.entrySet()) {
            Long userId = entry.getKey();
            List<String> deviceIds = entry.getValue();
            
            if (deviceIds != null && !deviceIds.isEmpty()) {
                for (String deviceId : deviceIds) {
                    try {
                        sendToUserDevice(userId, deviceId, signaling);
                        totalDevices++;
                    } catch (Exception e) {
                        log.error("发送消息给用户设备失败: userId={}, deviceId={}", userId, deviceId, e);
                    }
                }
            }
        }
        
        log.debug("发送消息给指定用户设备完成: userCount={}, deviceCount={}, msgId={}, type={}", 
            userDevices.size(), totalDevices, signaling.getMessageId(), signaling.getType());
    }

    @Override
    public void broadcastToParticipants(Long sessionId, SignalingMessage signaling) {
        broadcastToParticipants(sessionId, signaling, null);
    }

    @Override
    public void broadcastToParticipants(Long sessionId, SignalingMessage signaling, Long excludeUserId) {
        try {
            // 获取会话所有参与者（包含设备信息）
            List<CallParticipant> participants = callSessionService.getSessionParticipants(sessionId);
            
            if (participants.isEmpty()) {
                log.warn("会话没有参与者: sessionId={}", sessionId);
                return;
            }
            
            // 按用户分组设备
            Map<Long, List<String>> userDevices = participants.stream()
                .filter(participant -> !participant.getUserId().equals(excludeUserId))
                .collect(Collectors.groupingBy(
                    CallParticipant::getUserId,
                    Collectors.mapping(CallParticipant::getDeviceId, Collectors.toList())
                ));
            
            if (userDevices.isEmpty()) {
                log.debug("没有目标用户需要发送: sessionId={}, excludeUserId={}", sessionId, excludeUserId);
                return;
            }
            
            // 构建广播消息（设置正确的接收者）
            SignalingMessage broadcastMsg = SignalingMessage.newBuilder(signaling)
                .clearToUsers()
                .addAllToUsers(new ArrayList<>(userDevices.keySet()))
                .build();
            
            // 发送给所有目标用户的设备
            sendToUserDevices(userDevices, broadcastMsg);
            
            log.info("广播消息给会话参与者: sessionId={}, userCount={}, deviceCount={}, excludeUserId={}, type={}", 
                sessionId, userDevices.size(), 
                userDevices.values().stream().mapToInt(List::size).sum(),
                excludeUserId, signaling.getType());
                
        } catch (Exception e) {
            log.error("广播消息给会话参与者失败: sessionId={}", sessionId, e);
            throw new RuntimeException("广播消息失败: " + e.getMessage());
        }
    }

    @Override
    public void broadcastToParticipantDevices(Long sessionId, SignalingMessage signaling, 
                                             Map<Long, List<String>> excludeUserDevices) {
        try {
            // 获取会话所有参与者（包含设备信息）
            List<CallParticipant> participants = callSessionService.getSessionParticipants(sessionId);
            
            if (participants.isEmpty()) {
                log.warn("会话没有参与者: sessionId={}", sessionId);
                return;
            }
            
            // 过滤排除的设备
            Map<Long, List<String>> targetUserDevices = participants.stream()
                .collect(Collectors.groupingBy(
                    CallParticipant::getUserId,
                    Collectors.mapping(CallParticipant::getDeviceId, Collectors.toList())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        Long userId = entry.getKey();
                        List<String> deviceIds = entry.getValue();
                        List<String> excludeDevices = excludeUserDevices != null ? 
                            excludeUserDevices.get(userId) : Collections.emptyList();
                        
                        if (excludeDevices != null && !excludeDevices.isEmpty()) {
                            return deviceIds.stream()
                                .filter(deviceId -> !excludeDevices.contains(deviceId))
                                .collect(Collectors.toList());
                        }
                        return deviceIds;
                    }
                ));
            
            // 移除空设备列表的用户
            targetUserDevices.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
            if (targetUserDevices.isEmpty()) {
                log.debug("没有目标设备需要发送: sessionId={}", sessionId);
                return;
            }
            
            // 构建广播消息
            SignalingMessage broadcastMsg = SignalingMessage.newBuilder(signaling)
                .clearToUsers()
                .addAllToUsers(new ArrayList<>(targetUserDevices.keySet()))
                .build();
            
            // 发送给目标设备
            sendToUserDevices(targetUserDevices, broadcastMsg);
            
            log.info("广播消息给指定参与者设备: sessionId={}, userCount={}, deviceCount={}, type={}", 
                sessionId, targetUserDevices.size(),
                targetUserDevices.values().stream().mapToInt(List::size).sum(),
                signaling.getType());
                
        } catch (Exception e) {
            log.error("广播消息给参与者设备失败: sessionId={}", sessionId, e);
            throw new RuntimeException("广播消息失败: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(SignalingMessage signaling) {
        if (signaling.getToUsersList().isEmpty()) {
            log.warn("消息没有指定接收者: msgId={}", signaling.getMessageId());
            return;
        }
        
        sendToUsers(signaling.getToUsersList(), signaling);
    }

    @Override
    public void sendResponse(Long userId, String deviceId, int code, String message) {
        try {
            SignalingMessage responseMsg = SignalingMessage.newBuilder()
                .setMessageId(snowflakeIdGenerator.nextId())
                .setFromUser(0L) // 系统用户
                .setFromDevice("system")
                .addToUsers(userId)
                .setType(SignalingType.CALL_REJECT)
                .setTimestamp(System.currentTimeMillis())
                .setCallResponse(CallResponse.newBuilder()
                    .setAccepted(false)
                    .setReason(message)
                    .build())
                .build();
                
            if (deviceId != null) {
                sendToUserDevice(userId, deviceId, responseMsg);
            } else {
                sendToUser(userId, responseMsg);
            }
            
            log.debug("发送响应消息: userId={}, deviceId={}, code={}, message={}", 
                userId, deviceId, code, message);
            
        } catch (Exception e) {
            log.error("发送响应消息失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    @Override
    public List<String> getUserOnlineDevices(Long userId) {
        // 调用设备服务获取用户在线设备列表
        return deviceService.getUserOnlineDevices(userId);
    }
    
    /**
     * 发送振铃消息到选定的设备
     */
    @Override
    public void sendRingingToSelectedDevice(Long sessionId, Long userId, String selectedDeviceId, 
                                           List<String> allRingingDevices) {
        try {
            Ringing ringing = Ringing.newBuilder()
                .setDeviceId(selectedDeviceId)
                .setRingStartTime(System.currentTimeMillis())
                .addAllRingingDevices(allRingingDevices)
                .build();
            
            SignalingMessage signaling = SignalingMessage.newBuilder()
                .setMessageId(snowflakeIdGenerator.nextId())
                .setSessionId(sessionId)
                .setFromUser(0L)
                .setFromDevice("system")
                .addToUsers(userId)
                .setType(SignalingType.RINGING)
                .setTimestamp(System.currentTimeMillis())
                .setRinging(ringing)
                .build();
            
            // 只发送给选定的设备
            sendToUserDevice(userId, selectedDeviceId, signaling);
            
            log.info("发送振铃消息到选定设备: sessionId={}, userId={}, selectedDevice={}", 
                sessionId, userId, selectedDeviceId);
                
        } catch (Exception e) {
            log.error("发送振铃消息失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }
    
    /**
     * 发送媒体信令到特定设备
     */
    @Override
    public void sendMediaSignalingToDevice(Long sessionId, Long fromUser, String fromDevice, 
                                          Long toUser, String toDevice, SignalingType type, Object payload) {
        try {
            SignalingMessage.Builder builder = SignalingMessage.newBuilder()
                .setMessageId(snowflakeIdGenerator.nextId())
                .setSessionId(sessionId)
                .setFromUser(fromUser)
                .setFromDevice(fromDevice)
                .addToUsers(toUser)
                .setType(type)
                .setTimestamp(System.currentTimeMillis());
            
            // 根据类型设置对应的payload
            // todo
            SignalingMessage signaling = builder.build();
            
            // 发送到特定设备
            sendToUserDevice(toUser, toDevice, signaling);
            
            log.debug("发送媒体信令到设备: sessionId={}, fromUser={}, toUser={}, toDevice={}, type={}", 
                sessionId, fromUser, toUser, toDevice, type);
                
        } catch (Exception e) {
            log.error("发送媒体信令到设备失败: sessionId={}, toUser={}, toDevice={}", 
                sessionId, toUser, toDevice, e);
        }
    }
}