package com.guru.im.common.constant;

public enum OfflineSyncStatus {
    PENDING(1, "进行中"),
    PUSHED(2, "已推送MQ"),
    DELIVERED(3, "已送达"),
    PUSH_FAILED(4, "推送MQ失败"),
    FAILED(2, "送达失败");

    private final int code;
    private final String desc;

    OfflineSyncStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}