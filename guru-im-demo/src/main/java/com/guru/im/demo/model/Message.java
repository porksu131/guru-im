package com.guru.im.demo.model;

import com.guru.im.demo.gui.file.model.FileTransfer;

public class Message {
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAIL = -1;

    public static final int SEND_STATUS_SENDING = 1;
    public static final int SEND_STATUS_SUCCESS = 2;
    public static final int SEND_STATUS_FAILURE = 3;
    public static final int SEND_STATUS_UPLOADING = 4;
    public static final int SEND_STATUS_UPLOAD_PAUSE = 5;
    public static final int SEND_STATUS_UPLOAD_FAILED = 6;
    public static final int SEND_STATUS_DOWNLOADING = 7;
    public static final int SEND_STATUS_DOWNLOAD_PAUSE = 8;
    public static final int SEND_STATUS_DOWNLOADED = 9;
    public static final int SEND_STATUS_DOWNLOAD_FAILED = 10;

    public static final int READ_STATUS_UNREAD = 1;
    public static final int READ_STATUS_READ = 2;

    private Long messageId; //  服务端消息唯一id snowflake
    private String messageContent; //  消息内容
    private long receiverId; //  消息接收者id
    private String receiverName; //  消息接收者名称
    private long senderId; //  消息发送者id
    private String senderName; //  消息发送者名称
    private int messageType; //  消息类型 1:文字消息 2:图片消息 3:语音消息 4:视频 5:文件 6:表情 7:位置
    private String clientMsgId; //  客户端消息id
    private long clientSendTime; //  客户端消息发送时间
    private Long clientSeq; // 客户端临时生成的本地序列号
    private Long serverSeq; // 服务器分配的会话中全局严格连续序列号
    private Long timestamp; // 服务器时间
    private int conversationType; // 会话类型
    private Long conversationId; // 会话id
    private int readStatus; //  消息状态 1:未读   2:已读
    private int sendStatus; //  消息发送状态 1:发送中 2:发送成功，3:发送失败
    private int retryCount; // 重发次数

    private MediaInfo mediaInfo; // 多媒体消息包装类，由messageContent反序列化得到

    private FileTransfer uploadTransfer; // 文件上传进度
    private FileTransfer downloadTransfer; // 文件下载进度

    private boolean isLocalTemp; // 是否是本地临时消息
    public Message() {
    }

    public String generateSessionId() {
        return generateSessionId(senderId, receiverId);
    }

    public static String generateSessionId(long messageFrom, long messageTo) {
        long min = Math.min(messageFrom, messageTo);
        long max = Math.max(messageFrom, messageTo);
        return min + "-" + max;
    }

    public boolean isOwnerMessage(Long userId) {
        return userId.equals(senderId);
    }

    // getters and setters
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }
    public long getClientSendTime() { return clientSendTime; }
    public void setClientSendTime(long clientSendTime) { this.clientSendTime = clientSendTime; }
    public int getSendStatus() {
        return sendStatus;
    }
    public void setSendStatus(int sendStatus) {
        this.sendStatus = sendStatus;
    }
    public String getReceiverName() {
        return receiverName;
    }
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }
    public int getReadStatus() {
        return this.readStatus;
    }
    public Long getServerSeq() {
        return serverSeq;
    }
    public void setServerSeq(Long serverSeq) {
        this.serverSeq = serverSeq;
    }
    public boolean isLocalTemp() {
        return isLocalTemp;
    }
    public void setLocalTemp(boolean localTemp) {
        isLocalTemp = localTemp;
    }
    public Long getClientSeq() {
        return clientSeq;
    }
    public void setClientSeq(Long clientSeq) {
        this.clientSeq = clientSeq;
    }
    public String getClientMsgId() {
        return clientMsgId;
    }
    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }
    public Long getConversationId() {
        return conversationId;
    }
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    public int getRetryCount() {
        return retryCount;
    }
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    public Long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public int getConversationType() {
        return conversationType;
    }
    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }
    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }
    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }
    public FileTransfer getUploadTransfer() {
        return uploadTransfer;
    }
    public void setUploadTransfer(FileTransfer uploadTransfer) {
        this.uploadTransfer = uploadTransfer;
    }
    public FileTransfer getDownloadTransfer() {
        return downloadTransfer;
    }
    public void setDownloadTransfer(FileTransfer downloadTransfer) {
        this.downloadTransfer = downloadTransfer;
    }
}

