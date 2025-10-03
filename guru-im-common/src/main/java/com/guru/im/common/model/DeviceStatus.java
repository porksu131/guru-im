package com.guru.im.common.model;

public class DeviceStatus {
    private String deviceId;
    private String gatewayAddress;
    private String clientVersion;
    private Long lastActiveTime;
    private String platform;
    private Integer onlineStatus;

    public DeviceStatus() {}

    public DeviceStatus(String deviceId, String gatewayAddress,
                        String clientVersion, Long lastActiveTime, String platform, Integer onlineStatus) {
        this.deviceId = deviceId;
        this.gatewayAddress = gatewayAddress;
        this.clientVersion = clientVersion;
        this.lastActiveTime = lastActiveTime;
        this.platform = platform;
        this.onlineStatus = onlineStatus;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public Long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }
}