package com.guru.im.sdk.service;

import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.protocol.model.OfflineSyncRequest;
import com.guru.im.protocol.model.SyncEventRequest;

public interface UserService {
    void addFriend(long userId);

    void removeFriend(long userId);

    void getUserInfo(long userId);

    void updateUserStatus(String status);
    // 其他用户相关方法...

    boolean logout() throws Exception;

    void sendSyncMessageRequest(OfflineSyncRequest offlineSyncRequest, MessageSendCallback sendCallback) throws Exception;

    void sendSyncEventsRequest(SyncEventRequest syncEventRequest, MessageSendCallback sendCallback) throws Exception;
}