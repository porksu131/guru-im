package com.guru.im.protocol.util;

import com.google.protobuf.ByteString;
import com.guru.im.protocol.model.*;

import java.lang.System;
import java.util.UUID;

public class MessageBuilder {
    public static SequenceIdUtil sequenceIdUtil = SequenceIdUtil.getInstance();

    public static Response createResponse(int code, String msg) {
        return Response.newBuilder()
                .setCode(code)
                .setMsg(msg)
                .build();
    }

    public static Response createResponse(int code, String msg,  byte[] body) {
        return Response.newBuilder()
                .setCode(code)
                .setMsg(msg)
                .setData(ByteString.copyFrom(body))
                .build();
    }

    public static ImMessage createImResponse(ImMessage request, int code, String msg, byte[] body) {
        Response response = Response.newBuilder()
                .setCode(code)
                .setMsg(msg)
                .setData(ByteString.copyFrom(body))
                .build();
        return createImResponse(request, response);
    }

    public static ImMessage createImResponse(ImMessage request, int code, String msg) {
        Response response = Response.newBuilder()
                .setCode(code)
                .setMsg(msg)
                .build();
        return createImResponse(request, response);
    }

    public static ImMessage createImResponse(ImMessage request, Response response) {
        return ImMessage.newBuilder()
                .setMsgId(request.getMsgId())
                .setVersion(request.getVersion())
                .setMsgType(ImMessage.MsgType.RESPONSE)
                .setMessageType(request.getMessageType())
                .setResponse(response)
                .build();
    }

    public static ImMessage createAuthMessage(String token) {
        AuthMessage authMessage = AuthMessage.newBuilder()
                .setToken(token)
                .setAuthTime(System.currentTimeMillis())
                .build();
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setAuthMessage(authMessage)
                .build();
    }

    public static ImMessage createHeartbeat() {
       HeartbeatMessage heartbeatMessage = HeartbeatMessage.newBuilder()
               .setSendTime(System.currentTimeMillis())
               .build();
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .setMsgType(ImMessage.MsgType.ONEWAY)
                .setHeartbeatMessage(heartbeatMessage)
                .build();
    }

    public static ImMessage createIMChatMessage(ChatMessage chatMessage) {
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.CHAT)
                .setChatMessage(chatMessage)
                .build();
    }

    public static ImMessage createIMReadReceiptReq(ReadReceiptReq readReceiptReq) {
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.READ)
                .setReadReceiptReq(readReceiptReq)
                .build();
    }

    public static ChatMessage createChatMessageHeader(long senderId, long receiverId, long conversationId,
                                                      long clientSeq, String clientMsgId, DeviceInfo deviceInfo) {
        return ChatMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setDirection(MessageDirection.REQUEST)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setClientSeq(clientSeq)
                .setClientMsgId(clientMsgId)
                .setConversationType(ConversationType.PRIVATE)
                .setConversationId(conversationId)
                .setDeviceInfo(deviceInfo)
                .setTraceId(UUID.randomUUID().toString())
                .build();
    }

    public static ImMessage createPresenceNotify(PresenceNotify presenceNotify) {
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .setMsgType(ImMessage.MsgType.ONEWAY)
                .setPresenceNotify(presenceNotify)
                .build();
    }

    public static ImMessage createDefaultImMessage() {
        return ImMessage.newBuilder()
                .setMsgId(sequenceIdUtil.next())
                .setVersion(1)
                .build();
    }
}