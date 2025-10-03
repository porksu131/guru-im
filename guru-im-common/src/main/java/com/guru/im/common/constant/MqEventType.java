package com.guru.im.common.constant;

public class MqEventType {
//    SINGLE_CHAT_BEGIN(1, "发起单聊"),
//    SINGLE_CHAT_DISPATCH(2, "转发消息"),
//    SINGLE_CHAT_DISPATCH_SUCCESS(3, "消息转发成功"),
//    SINGLE_CHAT_DISPATCH_FAIL(4, "消息转发失败"),
//    BATCH_MSG_DISPATCH(5, "转发批量消息"),
//    BATCH_MSG_DISPATCH_SUCCESS(6, "批量消息转发成功"),
//    BATCH_MSG_DISPATCH_FAIL(7, "批量消息转发失败"),
//    USER_ONLINE_CHANGE_NOTIFY(8, "用户在线状态变更通知"),
//    FRIEND_NOTIFY_DISPATCH(9, "好友通知"),
//    FRIEND_NOTIFY_DISPATCH_SUCCESS(10, "好友通知成功"),
//    FRIEND_NOTIFY_DISPATCH_FAIL(11, "好友通知失败"),
//    BATCH_FRIEND_NOTIFY_DISPATCH(12, "转发批量好友通知"),
//    BATCH_FRIEND_NOTIFY_DISPATCH_SUCCESS(13, "批量好友通知转发成功"),
//    BATCH_FRIEND_NOTIFY_DISPATCH_FAIL(14, "批量好友通知转发失败"),

    public static final int CREATE_MESSAGE = 1; // 创建消息
    public static final int DISPATCH_MESSAGE = 2; // 转发消息
    public static final int DISPATCH_MESSAGE_ONEWAY = 3; // 转发消息（单向）
    public static final int DISPATCH_SUCCESS = 4; // 转发消息成功
    public static final int DISPATCH_FAIL = 5; // 转发消息失败


    private final int code;
    private final String desc;

    MqEventType(int code, String desc) {
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
