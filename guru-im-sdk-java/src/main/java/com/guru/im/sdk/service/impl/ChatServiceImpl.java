package com.guru.im.sdk.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.ChatMessageSendCallBack;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.model.UserInfo;
import com.guru.im.sdk.service.ChatService;
import com.guru.im.sdk.service.DefaultInvokeCallback;

public class ChatServiceImpl implements ChatService {
    private final long sendTimeOut;
    private final IMClient imClient;
    private final UserInfo userInfo;
    private final DeviceInfo deviceInfo;

    public ChatServiceImpl(IMClientManager clientManager) {
        this.sendTimeOut = clientManager.getClient().getConfig().getSendTimeout();
        this.imClient = clientManager.getClient();
        this.userInfo = clientManager.getUserInfo();
        this.deviceInfo = clientManager.getDeviceInfo();
    }

    @Override
    public void sendChatMessage(ChatMessage chatMessage, ChatMessageSendCallBack sendCallBack)
            throws Exception {
        ImMessage imMessage = MessageBuilder.createIMChatMessage(chatMessage);
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new InvokeCallback() {
            @Override
            public void operationFail(Throwable throwable) {
                sendCallBack.onSendFail(throwable.getMessage());
            }

            @Override
            public void operationSucceed(ImMessage imMessage) {
                Response response = imMessage.getResponse();
                if (imMessage.getResponse().getCode() == ResponseCode.SUCCESS) {
                    try {
                        ChatMessageAck ack = ChatMessageAck.parseFrom(imMessage.getResponse().getData());
                        sendCallBack.onSendSuccess(ack);
                    } catch (InvalidProtocolBufferException e) {
                        sendCallBack.onSendFail("parse chat message ack error:" + e.getMessage());
                    }
                } else {
                    sendCallBack.onSendFail(response.getMsg());
                }
            }
        });
    }

    @Override
    public void sendReadReceiptReq(ReadReceiptReq readReceiptReq, MessageSendCallback sendCallBack)
            throws Exception {
        ImMessage imMessage = MessageBuilder.createIMReadReceiptReq(readReceiptReq);
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new DefaultInvokeCallback(sendCallBack));
    }
}
