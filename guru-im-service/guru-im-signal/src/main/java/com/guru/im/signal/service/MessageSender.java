package com.guru.im.signal.service;

import com.google.protobuf.GeneratedMessageV3;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;

import java.util.List;
import java.util.Map;

public interface MessageSender {

    /**
     * 发送消息给指定用户的所有设备
     */
    void sendToUser(Long userId, SignalingMessage signaling);

    /**
     * 发送消息给指定用户的特定设备
     */
    void sendToUserDevice(Long userId, String deviceId, SignalingMessage signaling);

    /**
     * 发送消息给多个用户的所有设备
     */
    void sendToUsers(List<Long> userIds, SignalingMessage signaling);

    /**
     * 发送消息给多个用户的特定设备
     */
    void sendToUserDevices(Map<Long, List<String>> userDevices, SignalingMessage signaling);

    /**
     * 广播消息给会话所有参与者的所有设备
     */
    void broadcastToParticipants(Long sessionId, SignalingMessage signaling);

    /**
     * 广播消息给会话所有参与者的所有设备（排除指定用户）
     */
    void broadcastToParticipants(Long sessionId, SignalingMessage signaling, Long excludeUserId);

    /**
     * 广播消息给会话所有参与者的特定设备
     */
    void broadcastToParticipantDevices(Long sessionId, SignalingMessage signaling, Map<Long, List<String>> excludeUserDevices);

    /**
     * 发送原始ImMessage
     */
    void sendMessage(SignalingMessage signaling);

    /**
     * 发送响应消息
     */
    void sendResponse(Long userId, String deviceId, int code, String message);

    /**
     * 获取用户的在线设备列表
     */
    List<String> getUserOnlineDevices(Long userId);

    /**
     * 发送振铃消息到指定设备
     */
    void sendRingingToSelectedDevice(Long id, Long toUser, String primaryDevice, List<String> allDevices);

    /**
     * 发送信令消息到指定设备
     */
    void sendMediaSignalingToDevice(Long sessionId, Long fromUser, String fromDevice, Long userId, String deviceId, SignalingType type, Object payload);
}