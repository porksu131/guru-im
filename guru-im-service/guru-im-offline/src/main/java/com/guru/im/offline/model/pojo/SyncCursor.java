package com.guru.im.offline.model.pojo;

import java.io.Serializable;

/**
 * 同步位点实体类，用于记录各会话的同步进度
 * 实现序列化接口支持对象持久化
 */
public class SyncCursor implements Serializable {

    /**
     * 记录ID（雪花ID）
     * 主键，采用分布式ID生成算法
     */
    private Long id;

    /**
     * 用户ID
     * 标识消息同步的目标用户
     */
    private Long userId;

    /**
     * 设备ID
     * 用户终端设备标识
     */
    private String deviceId;

    /**
     * 会话类型
     * 1:单聊 2:群聊 3:系统通知
     */
    private Integer conversationType;

    /**
     * 会话ID
     * 标识具体的对话上下文
     */
    private Long conversationId;

    /**
     * 最后同步序列号
     * 记录该会话最后同步的消息序号
     */
    private Long lastSyncSeq;

    /**
     * 最后同步时间
     * 记录该会话最后同步的时间戳（毫秒）
     */
    private Long lastSyncTime;

    /**
     * 同步版本
     * 用于处理冲突的乐观锁机制
     */
    private Integer syncVersion;

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

    public Integer getConversationType() {
        return conversationType;
    }

    public void setConversationType(Integer conversationType) {
        this.conversationType = conversationType;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
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

    public Integer getSyncVersion() {
        return syncVersion;
    }

    public void setSyncVersion(Integer syncVersion) {
        this.syncVersion = syncVersion;
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
