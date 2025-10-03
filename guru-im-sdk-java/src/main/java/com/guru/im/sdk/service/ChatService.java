package com.guru.im.sdk.service;

import com.guru.im.core.common.listener.ChatMessageSendCallBack;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.ReadReceiptReq;

public interface ChatService {
    void sendChatMessage(ChatMessage chatMessage, ChatMessageSendCallBack chatMessageSendCallBack) throws Exception;

    void sendReadReceiptReq(ReadReceiptReq readReceiptReq, MessageSendCallback messageSendCallBack) throws Exception;
}
