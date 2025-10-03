package com.guru.im.demo.gui.file.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileTransfer {
    public enum Status {
        PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    private String id;
    private String clientMsgId;
    private Long conversationId;
    private Long fileId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private Integer fileType;
    private int transferType; // 0: upload, 1: download
    private String fileSavePath; // 下载保存路径
    //private int progress;
    private String fileUrl;
    private boolean paused;
    private boolean cancelled;
    private String thumbnailUrl;
    private String thumbnailPath;
    private Integer originWidth;
    private Integer originHeight;
    private Long duration;
    private Integer audioCodec;
    private Integer audioBitrate;
    private Integer videoCodec;
    private Integer videoBitrate;
    private String md5;
    private String contentType;
    private Boolean completeThumbnail;

    private Date createdAt;
    private Date updatedAt;


    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);

    public FileTransfer() {
    }

    public FileTransfer(int fileType, File file) {
        this.id = UUID.randomUUID().toString();
        this.fileType = fileType;
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.status.set(Status.PENDING);
        this.progress.set(0);
        this.transferType = 0; //默认上传
    }

    // 安全的状体转换方法
    public boolean trySetStatus(Status newStatus) {
        Status current = status.get();

        // 定义合法状态转换
        if (isValidTransition(current, newStatus)) {
            return status.compareAndSet(current, newStatus);
        }
        return false;
    }

    private boolean isValidTransition(Status current, Status newStatus) {
        // 完成或取消后不能改变状态
        if (current == Status.COMPLETED || current == Status.CANCELLED) {
            return false;
        }

        return true;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTransferType() {
        return transferType;
    }

    public void setTransferType(int transferType) {
        this.transferType = transferType;
    }

    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status status) {
        this.status.compareAndSet(this.status.get(), status);
    }

    public int getProgress() {
        return progress.get();
    }

    public void setProgress(int progress) {
        this.progress.set(progress);
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Boolean getCompleteThumbnail() {
        return completeThumbnail;
    }

    public void setCompleteThumbnail(Boolean completeThumbnail) {
        this.completeThumbnail = completeThumbnail;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFileSavePath() {
        return fileSavePath;
    }

    public void setFileSavePath(String fileSavePath) {
        this.fileSavePath = fileSavePath;
    }

    public Integer getOriginWidth() {
        return originWidth;
    }

    public void setOriginWidth(Integer originWidth) {
        this.originWidth = originWidth;
    }

    public Integer getOriginHeight() {
        return originHeight;
    }

    public void setOriginHeight(Integer originHeight) {
        this.originHeight = originHeight;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(Integer audioCodec) {
        this.audioCodec = audioCodec;
    }

    public Integer getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(Integer audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public Integer getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(Integer videoCodec) {
        this.videoCodec = videoCodec;
    }

    public Integer getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }
}