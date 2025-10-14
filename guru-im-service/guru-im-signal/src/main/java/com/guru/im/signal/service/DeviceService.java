package com.guru.im.signal.service;

import java.util.List;

public interface DeviceService {
    
    /**
     * 获取用户在线设备列表
     */
    List<String> getUserOnlineDevices(Long userId);
    
    /**
     * 获取用户主要设备（用于振铃选择）
     */
    String getUserPrimaryDevice(Long userId);
}