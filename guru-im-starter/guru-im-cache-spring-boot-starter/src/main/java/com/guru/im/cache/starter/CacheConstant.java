package com.guru.im.cache.starter;

/**
 * redis
 */
public class CacheConstant {


    public static final String LOGIN_USER = "user:loginUser:";

    public static final String CAPTCHA = "user:captcha:";

    public static final String USER_GATEWAY_SERVER = "user:gatewayServer:";

    public static final String USER_DEVICE_GATEWAY_SERVER = "user:device:gatewayServer:";

    public static final String MESSAGE_ID = "chat:messageId:";

    public static final String CHAT_SERVER_CLIENT_COUNT = "chat:server:clientCount:";

    public static final String SNOWFLAKE_WORKER_ID = "snowflake:workerId:";

    public static final String JWT_REFRESH_TOKEN = "jwt:refreshToken:";

    public static final String JWT_BLACK_LIST = "jwt:blackList:";

    public static final String OFFLINE_MSG_ID_LIST = "offline:msgIdList:";

    public static final String OFFLINE_FRIEND_NOTIFY_ID_LIST = "offline:friendNotifyIdList:";

    public static final String CONVERSATION_SEQ = "chat:conversationSeq:";

    public static final String IM_GLOBAL_SEQ = "im:globalSeq";

    public static final String USER_ONLINE_STATUS = "user:onlineStatus:";

    public static final String USER_ACTIVE_TIME = "user:activeTime:";
}