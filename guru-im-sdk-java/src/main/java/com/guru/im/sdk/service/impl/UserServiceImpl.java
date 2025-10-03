package com.guru.im.sdk.service.impl;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.model.UserInfo;
import com.guru.im.sdk.service.DefaultInvokeCallback;
import com.guru.im.sdk.service.UserService;

import java.lang.System;

public class UserServiceImpl implements UserService {
    private final long sendTimeOut;
    private final IMClient imClient;
    private final UserInfo userInfo;
    private final DeviceInfo deviceInfo;

    public UserServiceImpl(IMClientManager clientManager) {
        this.sendTimeOut = clientManager.getClient().getConfig().getSendTimeout();
        this.imClient = clientManager.getClient();
        this.userInfo = clientManager.getUserInfo();
        this.deviceInfo = clientManager.getDeviceInfo();
    }

    @Override
    public void addFriend(long userId) {

    }

    @Override
    public void removeFriend(long userId) {

    }

    @Override
    public void getUserInfo(long userId) {

    }

    @Override
    public void updateUserStatus(String status) {

    }

    @Override
    public boolean logout() throws Exception {
        AuthMessage authMessage = AuthMessage.newBuilder()
                .setDeviceInfo(deviceInfo)
                .setAuthType(AuthType.AUTH_LOGOUT)
                .setAuthTime(System.currentTimeMillis())
                .setToken(userInfo.getAccessToken())
                .build();

        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.AUTH)
                .setAuthMessage(authMessage)
                .build();
        ImMessage imResult = imClient.sendMessageSync(imMessage, sendTimeOut);
        return imResult.getResponse().getCode() == ResponseCode.SUCCESS;
    }

    @Override
    public void sendSyncMessageRequest(OfflineSyncRequest offlineSyncRequest, MessageSendCallback sendCallback)
            throws Exception {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.OFFLINE_MSG_REQUEST)
                .setSyncRequest(offlineSyncRequest)
                .build();
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new DefaultInvokeCallback(sendCallback));
    }

    @Override
    public void sendSyncEventsRequest(SyncEventRequest syncEventRequest, MessageSendCallback sendCallback) throws Exception {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.OFFLINE_EVENT_REQUEST)
                .setSyncEventRequest(syncEventRequest)
                .build();
        this.imClient.sendMessageAsync(imMessage, sendTimeOut, new DefaultInvokeCallback(sendCallback));
    }
}
