package com.guru.im.offline.redis;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyManager {
    
    // 同步位点Key: sync_cursor:{userId}:{deviceId}:{convType}_{convId}
    public String getCursorKey(Long userId, String deviceId, Integer convType, String convId) {
        return String.format("sync_cursor:%d:%s:%d_%s", userId, deviceId, convType, convId);
    }
    
    public String getCursorKey(Long userId, String deviceId, String convKey) {
        return String.format("sync_cursor:%d:%s:%s", userId, deviceId, convKey);
    }
    
    // 同步会话Key: sync_session:{syncId}
    public String getSessionKey(Long syncId) {
        return String.format("sync_session:%s", syncId);
    }
    
    // 批次数据Key: sync_batch:{syncId}:{batchNo}
    public String getBatchKey(String syncId, Integer batchNo) {
        return String.format("sync_batch:%s:%d", syncId, batchNo);
    }
    
    // 用户设备集合Key: user_devices:{userId}
    public String getUserDevicesKey(Long userId) {
        return String.format("user_devices:%d", userId);
    }
    
    // 同步锁Key: lock:sync:{userId}:{deviceId}
    public String getLockKey(Long userId, String deviceId) {
        return String.format("lock:sync:%d:%s", userId, deviceId);
    }
}