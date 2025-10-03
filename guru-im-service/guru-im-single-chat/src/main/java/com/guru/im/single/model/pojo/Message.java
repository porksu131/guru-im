package com.guru.im.single.model.pojo;

/**
 * 消息实体类
 */
public class Message {
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 客户端消息ID
     */
    private String clientMsgId;
    
    /**
     * 会话类型
     */
    private Integer conversationType;
    
    /**
     * 会话ID
     */
    private Long conversationId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 消息内容
     */
    private String messageContent;
    
    /**
     * 服务端序列号
     */
    private Long serverSeq;
    
    /**
     * 客户端序列号
     */
    private Long clientSeq;


    private Long clientSendTime;
    /**
     * '@'的用户列表
     */
    private String atUsers;
    
    /**
     * 是否已撤回
     */
    private Boolean isRecalled;
    
    /**
     * 撤回时间
     */
    private Long recallTime;
    
    /**
     * 已读人数
     */
    private Integer readCount;

    /**
     * 推送状态: 0:未推送 1:已推送，待送达 2:推送失败 3:已送达
     */
    private Integer deliveryStatus;

    /**
     * 送达时间
     */
    private Long deliveryTime;

    /**
     * 推送重试次数
     */
    private Integer retryCount;

    /**
     * 最后重试时间
     */
    private Long lastRetryTime;
    
    /**
     * 消息状态(0:删除 1:正常)
     */
    private Integer status;
    
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

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Long getServerSeq() {
        return serverSeq;
    }

    public void setServerSeq(Long serverSeq) {
        this.serverSeq = serverSeq;
    }

    public Long getClientSeq() {
        return clientSeq;
    }

    public void setClientSeq(Long clientSeq) {
        this.clientSeq = clientSeq;
    }

    public String getAtUsers() {
        return atUsers;
    }

    public void setAtUsers(String atUsers) {
        this.atUsers = atUsers;
    }

    public Boolean getRecalled() {
        return isRecalled;
    }

    public void setRecalled(Boolean recalled) {
        isRecalled = recalled;
    }

    public Long getRecallTime() {
        return recallTime;
    }

    public void setRecallTime(Long recallTime) {
        this.recallTime = recallTime;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Integer getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(Integer deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Long getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Long deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getLastRetryTime() {
        return lastRetryTime;
    }

    public void setLastRetryTime(Long lastRetryTime) {
        this.lastRetryTime = lastRetryTime;
    }

    public Long getClientSendTime() {
        return clientSendTime;
    }

    public void setClientSendTime(Long clientSendTime) {
        this.clientSendTime = clientSendTime;
    }
}
