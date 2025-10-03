package com.guru.im.demo.model;

import com.guru.im.protocol.model.RequestStatus;

// 通知消息实体类
public class Notification {
    public enum Type {
        SYS_UPDATE(1, "系统更新"),
        FRIEND_REQUEST(2, "好友请求"),
        GROUP_INVITE(3, "群聊邀请"),
        SECURITY_NOTIFY(4, "安全提醒"),
        BIRTHDAY_NOTIFY(5, "生日提醒");
        //        FRIEND_REQUEST_RESULT(6, "好友申请结果");
        private int code;
        private String des;

        Type(int code, String des) {
            this.code = code;
            this.des = des;
        }

        public static Type parse(Integer type) {
            final Type[] values = Type.values();
            for (Type value : values) {
                if (value.code == type) {
                    return value;
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }

        public String getDes() {
            return des;
        }
    }

    private Long id;
    private Integer type;
    private Long correlationId;
    private String title;
    private String content;
    private Long timestamp;
    private Boolean isRead;

    // 非数据库字段
    private FriendRequest friendRequest;
    private GroupInvite groupInvite;


    public Notification() {
    }

    public Type getTypeEnum() {
        return Type.parse(this.type);
    }

    public String getSenderName(long userId) {
        if (type == null) {
            return "";
        }
        if (type == Type.FRIEND_REQUEST.getCode()) {
            if (friendRequest == null) {
                return "";
            }
            if (userId == friendRequest.getRequesterId()) {
                // 自己发送的好友申请
                return "您";
            } else {
                // 其他人发送的好友申请
                return friendRequest.getRequesterName();
            }
        } else if (type == Type.GROUP_INVITE.getCode()) {
            return groupInvite == null ? "" : groupInvite.getInviterName();
        } else {
            return "系统";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(Long correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public FriendRequest getFriendRequest() {
        return friendRequest;
    }

    public void setFriendRequest(FriendRequest friendRequest) {
        this.friendRequest = friendRequest;
    }

    public GroupInvite getGroupInvite() {
        return groupInvite;
    }

    public void setGroupInvite(GroupInvite groupInvite) {
        this.groupInvite = groupInvite;
    }

}

