package com.guru.im.protocol.util;

import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;

import java.util.UUID;

public class MessageUpdater {

    public static ImMessage updateSequenceIds(ImMessage imMessage, long newMessageId, long newServerSeq) {
        ImMessage.Builder imBuilder = imMessage.toBuilder();

        ChatMessage chatMessage = imMessage.getChatMessage();
        ChatMessage.Builder builder = chatMessage.toBuilder();
        builder.setMessageId(newMessageId);
        builder.setServerSeq(newServerSeq);
        builder.setTimestamp(System.currentTimeMillis());
        builder.setTraceId(UUID.randomUUID().toString());

        imBuilder.setChatMessage(builder.build());
        return imBuilder.build();
    }

    public static ImMessage updateSequenceIdsForSignal(ImMessage imMessage, long newSessionId, long newMessageId) {
        ImMessage.Builder imBuilder = imMessage.toBuilder();

        SignalingMessage signalingMessage = imMessage.getSignalingMessage();
        SignalingMessage.Builder builder = signalingMessage.toBuilder();
        if (signalingMessage.getSessionId() == 0 && signalingMessage.getType() == SignalingType.INIT_SESSION) {
            builder.setSessionId(newSessionId);
        }
        builder.setMessageId(newMessageId);
        builder.setTimestamp(System.currentTimeMillis());

        imBuilder.setSignalingMessage(builder.build());
        return imBuilder.build();
    }

}