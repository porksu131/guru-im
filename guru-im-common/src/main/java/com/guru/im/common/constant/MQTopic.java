package com.guru.im.common.constant;

public class MQTopic {
    // 发往 单聊微服务 的 topic
    public static final String SING_CHAT_TOPIC = "sing-chat-topic";
    // 发往 群聊微服务 的 topic
    public static final String GROUP_CHAT_TOPIC = "group-chat-topic";
    // 发往 离线微服务 的 topic
    public static final String OFFLINE_TOPIC = "offline-topic";
    // 发往 用户微服务 的 topic
    public static final String USR_TOPIC = "user-topic";
    // 发往 信令微服务 的 topic
    public static final String SIGNAL_TOPIC = "signal-topic";

    // 发往 分发系统 的 topic （message类）
    public static final String DISPATCH_CHAT_TOPIC = "dispatch-chat-topic";
    // 发往 分发系统 的 topic （control类）
    public static final String DISPATCH_CONTROL_TOPIC = "dispatch-control-topic";
    // 发往 分发系统 的 topic （notify类）
    public static final String DISPATCH_NOTIFY_TOPIC = "dispatch-notify-topic";
    // 发往 分发系统 的 topic （action类）
    public static final String DISPATCH_ACTION_TOPIC = "dispatch-action-topic";
    // 发往 分发系统 的 topic （signal类）
    public static final String DISPATCH_SIGNAL_TOPIC = "dispatch-signal-topic";


    // 死信队列 的 topic
    public static final String DLQ_TOPIC = "dlq-topic";
    // 重试队列 的 topic
    public static final String RETRY_TOPIC = "retry-topic";
}
