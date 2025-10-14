package com.guru.im.signal.service;

import com.guru.im.cache.starter.UserSessionManager;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.model.DeviceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);
    @Autowired
    private UserSessionManager userSessionManager;

    @Override
    public List<String> getUserOnlineDevices(Long userId) {
        List<DeviceStatus> onlineDevices = userSessionManager.getUserDeviceStatus(userId, OnlineStatus.ONLINE);
        if (onlineDevices == null) {
            return new ArrayList<>();
        }
        return onlineDevices.stream().map(DeviceStatus::getDeviceId).toList();
    }

    @Override
    public String getUserPrimaryDevice(Long userId) {
        List<String> onlineDevices = getUserOnlineDevices(userId);
        return onlineDevices.isEmpty() ? null : onlineDevices.get(0); // 暂时取第一个
    }
}