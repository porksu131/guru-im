package com.guru.im.offline.model.pojo;

public class OfflineEventContent {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 全局序列号
     */
    private Long globalSeq;
    
    /**
     * 事件类型
     */
    private Integer eventType;
    
    /**
     * 事件内容（Protobuf二进制）
     */
    private byte[] eventContent;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
    
    /**
     * 过期时间
     */
    private Long expireTime;

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

    public Long getGlobalSeq() {
        return globalSeq;
    }

    public void setGlobalSeq(Long globalSeq) {
        this.globalSeq = globalSeq;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public byte[] getEventContent() {
        return eventContent;
    }

    public void setEventContent(byte[] eventContent) {
        this.eventContent = eventContent;
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

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }
}