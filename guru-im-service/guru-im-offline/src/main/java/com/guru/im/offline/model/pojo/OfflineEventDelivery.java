package com.guru.im.offline.model.pojo;

public class OfflineEventDelivery {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 上次同步序列号
     */
    private Long lastSyncSeq;
    
    /**
     * 上次同步时间
     */
    private Long lastSyncTime;
    
    /**
     * 投递状态
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
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getLastSyncSeq() {
        return lastSyncSeq;
    }

    public void setLastSyncSeq(Long lastSyncSeq) {
        this.lastSyncSeq = lastSyncSeq;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
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
}
