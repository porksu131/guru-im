package com.guru.im.common.constant;

public enum CorrelationType {
    CHAT_MESSAGE(1, "聊天信息"),
    READ_RECEIPT_NOTIFY(2, "已读回执通知"),
    PRESENCE_NOTIFY(3, "用户状态通知"),
    FRIEND_REQUEST_NOTIFY(4, "好友申请通知"),
    GROUP_INVITE_NOTIFY(5, "群邀请通知"),
    BATCH_OFFLINE_SYNC(6, "批量离线消息同步"),
    BATCH_EVENTS_SYNC(7, "批量离线事件同步");


    private final int code;
    private final String desc;

    CorrelationType(int code, String desc) {
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
