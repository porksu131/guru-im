package com.guru.im.demo.model;

public class DeviceInfo {
    private String deviceId;
    private String deviceName;
    private String clientVersion;
    private int platform;

    public DeviceInfo() {
    }

    public DeviceInfo(String deviceId, String deviceName, String clientVersion, int platform) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.clientVersion = clientVersion;
        this.platform = platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }
}
