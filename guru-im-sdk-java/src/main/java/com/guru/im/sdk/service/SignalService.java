package com.guru.im.sdk.service;

import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.core.common.listener.SignalMessageSendCallBack;
import com.guru.im.protocol.model.SignalingMessage;

public interface SignalService {
    void sendSignalingMessage(SignalingMessage signalingMessage, MessageSendCallback sendCallback) throws Exception;

    void sendSignalingMessageWithAck(SignalingMessage signalingMessage, SignalMessageSendCallBack sendCallback) throws Exception;
}
