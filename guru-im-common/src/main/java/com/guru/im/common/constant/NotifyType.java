package com.guru.im.common.constant;

public enum NotifyType {
    ON_LINE(1, "上线通知"),
    OFF_LINE(2, "下线通知"),
    ADD_FRIEND_REQUEST(3, "添加好友请求通知"),
    ADD_FRIEND_RESPONSE(4, "响应添加好友请求通知"),
    FRIEND_RELATION_CHANGE(5, "好友关系变更通知");

    private final int code;
    private final String desc;

    NotifyType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public boolean equals(int code) {
        return this.code == code;
    }

    public int getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}
