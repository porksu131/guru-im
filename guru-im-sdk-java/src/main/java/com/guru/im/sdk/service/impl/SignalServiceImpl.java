package com.guru.im.sdk.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.core.common.listener.SignalMessageSendCallBack;
import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.service.DefaultInvokeCallback;
import com.guru.im.sdk.service.SignalService;

public class SignalServiceImpl implements SignalService {
    private final long sendTimeOut;
    private final IMClient imClient;

    public SignalServiceImpl(IMClientManager clientManager) {
        this.sendTimeOut = clientManager.getClient().getConfig().getSendTimeout();
        this.imClient = clientManager.getClient();
    }

    @Override
    public void sendSignalingMessage(SignalingMessage signalingMessage, MessageSendCallback sendCallback) throws Exception {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.ONEWAY)//todo
                .setSignalingMessage(signalingMessage)
                .setMessageType(MessageType.SIGNALING)
                .build();
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new DefaultInvokeCallback(sendCallback));
    }

    @Override
    public void sendSignalingMessageWithAck(SignalingMessage signalingMessage, SignalMessageSendCallBack sendCallback) throws Exception {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setSignalingMessage(signalingMessage)
                .setMessageType(MessageType.SIGNALING)
                .build();
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new InvokeCallback() {
            @Override
            public void operationFail(Throwable throwable) {
                sendCallback.onSendFail(throwable.getMessage());
            }

            @Override
            public void operationSucceed(ImMessage imMessage) {
                Response response = imMessage.getResponse();
                if (imMessage.getResponse().getCode() == ResponseCode.SUCCESS) {
                    try {
                        SignalMessageAck ack = SignalMessageAck.parseFrom(imMessage.getResponse().getData());
                        sendCallback.onSendSuccess(ack);
                    } catch (InvalidProtocolBufferException e) {
                        sendCallback.onSendFail("parse signal message ack error:" + e.getMessage());
                    }
                } else {
                    sendCallback.onSendFail(response.getMsg());
                }
            }
        });
    }
}
