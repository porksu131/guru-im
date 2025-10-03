package com.guru.im.offline.model.pojo;

import java.io.Serializable;

/**
 * 离线消息实体类，用于存储用户未及时接收的离线消息数据
 * 实现序列化接口支持对象持久化
 */
public class OfflineMessage implements Serializable {

    /**
     * 记录ID（雪花ID）
     * 主键，采用分布式ID生成算法
     */
    private Long id;

    /**
     * 用户ID
     * 标识消息接收方
     */
    private Long userId;

    /**
     * 设备ID
     * 用户终端设备标识
     */
    private String deviceId;

    /**
     * 消息ID
     * 原始消息的唯一标识
     */
    private Long messageId;

    /**
     * 会话类型
     * 1:单聊 2:群聊 3:系统通知
     */
    private Integer conversationType;

    /**
     * 会话ID
     * 标识具体的对话上下文
     */
    private String conversationId;

    /**
     * 消息序列号
     * 用于保证消息时序性
     */
    private Long messageSeq;

    /**
     * 消息时间
     * 消息产生的时间戳（毫秒）
     */
    private Long messageTime;

    /**
     * 消息类型
     * 文本/图片/语音等消息类型编码
     */
    private Integer messageType;

    /**
     * 消息内容
     * JSON格式存储的消息体
     */
    private byte[] messageContent;

    /**
     * 消息优先级
     * 0:普通 1:重要 2:紧急
     */
    private Integer priority;

    /**
     * 过期时间
     * 消息失效的时间戳（毫秒）
     */
    private Long expireTime;

    /**
     * 同步版本号
     * 用于处理冲突的乐观锁机制
     */
    private Integer syncVersion;

    /**
     * 投递状态
     * 0:未投递 1:已投递 2:投递失败
     */
    private Integer deliveryStatus;

    /**
     * 投递次数
     * 记录消息重试次数
     */
    private Integer deliveryCount;

    /**
     * 最后投递时间
     * 最近一次投递的时间戳
     */
    private Long lastDeliveryTime;

    /**
     * 创建时间
     * 记录插入数据库的时间戳
     */
    private Long createTime;

    /**
     * 更新时间
     * 记录最后修改的时间戳
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

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Integer getConversationType() {
        return conversationType;
    }

    public void setConversationType(Integer conversationType) {
        this.conversationType = conversationType;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageSeq() {
        return messageSeq;
    }

    public void setMessageSeq(Long messageSeq) {
        this.messageSeq = messageSeq;
    }

    public Long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(Long messageTime) {
        this.messageTime = messageTime;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public byte[] getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(byte[] messageContent) {
        this.messageContent = messageContent;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getSyncVersion() {
        return syncVersion;
    }

    public void setSyncVersion(Integer syncVersion) {
        this.syncVersion = syncVersion;
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
