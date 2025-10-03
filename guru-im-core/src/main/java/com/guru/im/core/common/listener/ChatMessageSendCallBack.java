package com.guru.im.core.common.listener;


import com.guru.im.protocol.model.ChatMessageAck;

public interface ChatMessageSendCallBack {
    void onSendSuccess(ChatMessageAck chatMessageAck);

    void onSendFail(String errorMsg);
}
