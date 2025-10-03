package com.guru.im.protocol.util;

import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.ImMessage;

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

}