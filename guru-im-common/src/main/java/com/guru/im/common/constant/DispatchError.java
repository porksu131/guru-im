package com.guru.im.common.constant;

public enum DispatchError {
    NO_DEVICE_FOUND(0, "用户设备信息未找到"),
    USER_OFFLINE(1, "用户离线"),
    SYSTEM_ERROR(2, "系统异常");

    private final int code;
    private final String desc;

    DispatchError(int code, String desc) {
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