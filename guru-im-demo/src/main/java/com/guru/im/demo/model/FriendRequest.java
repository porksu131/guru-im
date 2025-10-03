package com.guru.im.demo.model;

import java.time.Instant;

public class FriendRequest {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requestMsg;
    private int requestStatus;
    private int requestType;
    private Long createTime;
    private Long responseTime;
    private Long responderId;
    private String responderName;

    public enum RequestStatus {
        PENDING(0), ACCEPTED(1), REJECTED(2), EXPIRED(3);

        private final int value;

        RequestStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RequestStatus fromValue(int value) {
            for (RequestStatus status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return PENDING;
        }
    }

    public enum RequestType {
        FRIEND_UNKNOWN(0), FRIEND_REQUEST(1), FRIEND_BLACK(2),
        FRIEND_DELETE(3), FRIEND_UN_BLACK(4);

        private final int value;

        RequestType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RequestType fromValue(int value) {
            for (RequestType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return FRIEND_UNKNOWN;
        }
    }

    public FriendRequest() {
    }

    public FriendRequest(Long requesterId, String requesterName, String requestMsg,
                         int requestType) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requestMsg = requestMsg;
        this.requestType = requestType;
        this.requestStatus = RequestStatus.PENDING.getValue();
        this.createTime = Instant.now().getEpochSecond();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequestMsg() {
        return requestMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    public int getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(int requestStatus) {
        this.requestStatus = requestStatus;
    }

    public int getRequestType() {
        return requestType;
    }

    public void setRequestType(int requestType) {
        this.requestType = requestType;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public Long getResponderId() {
        return responderId;
    }

    public void setResponderId(Long responderId) {
        this.responderId = responderId;
    }

    public String getResponderName() {
        return responderName;
    }

    public void setResponderName(String responderName) {
        this.responderName = responderName;
    }
}