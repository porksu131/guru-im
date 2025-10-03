package com.guru.im.offline.model.vo;

import com.guru.im.protocol.model.OfflineSyncType;

public class SyncEventRequest {
    private Long userId;
    private String deviceId;
    private Long lastSequence;
    private Integer syncSize;
    private OfflineSyncType syncType;
    private String clientVersion;
    private String networkType;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(Long lastSequence) {
        this.lastSequence = lastSequence;
    }

    public Integer getSyncSize() {
        return syncSize;
    }

    public void setSyncSize(Integer syncSize) {
        this.syncSize = syncSize;
    }

    public OfflineSyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(OfflineSyncType syncType) {
        this.syncType = syncType;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
}