package com.guru.im.offline.model.pojo;

public class OfflineMessageDelivery {
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 消息类型，1:聊天，2:好友请求
     */
    private int messageType;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 投递状态(0:未投递 1:已投递 2:投递失败)
     */
    private Integer deliveryStatus;
    
    /**
     * 投递次数
     */
    private Integer deliveryCount;
    
    /**
     * 最后投递时间
     */
    private Long lastDeliveryTime;

    /**
     * 错误码
     */
    private Integer errorCode;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;

    private OfflineMessageContent offlineMessageContent;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(Integer deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Integer getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(Integer deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public Long getLastDeliveryTime() {
        return lastDeliveryTime;
    }

    public void setLastDeliveryTime(Long lastDeliveryTime) {
        this.lastDeliveryTime = lastDeliveryTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public OfflineMessageContent getOfflineMessageContent() {
        return offlineMessageContent;
    }

    public void setOfflineMessageContent(OfflineMessageContent offlineMessageContent) {
        this.offlineMessageContent = offlineMessageContent;
    }
}