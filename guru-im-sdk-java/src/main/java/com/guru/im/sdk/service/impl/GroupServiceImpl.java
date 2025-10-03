package com.guru.im.sdk.service.impl;


import com.guru.im.core.im.IMClient;
import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.model.UserInfo;
import com.guru.im.sdk.service.GroupService;

import java.util.List;

public class GroupServiceImpl implements GroupService {
    private final long sendTimeOut;
    private final IMClient imClient;
    private final UserInfo userInfo;
    private final DeviceInfo deviceInfo;

    public GroupServiceImpl(IMClientManager clientManager) {
        this.sendTimeOut = clientManager.getClient().getConfig().getSendTimeout();
        this.imClient = clientManager.getClient();
        this.userInfo = clientManager.getUserInfo();
        this.deviceInfo = clientManager.getDeviceInfo();
    }

    @Override
    public void createGroup(String name, List<Long> memberIds) {
    }

    @Override
    public void sendGroupMessage(long groupId, String text) {
    }

    @Override
    public void addGroupMembers(long groupId, List<Long> memberIds) {
    }

    @Override
    public void removeGroupMembers(long groupId, List<Long> memberIds) {
    }
}