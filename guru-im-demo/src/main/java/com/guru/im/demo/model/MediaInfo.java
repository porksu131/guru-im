package com.guru.im.demo.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaInfo {

    // 图片格式
    public final int IMAGE_FORMAT_JPEG = 1;
    public final int IMAGE_FORMAT_PNG = 2;
    public final int IMAGE_FORMAT_GIF = 3;
    public final int IMAGE_FORMAT_WEBP = 4;

    // 文件类型
    public final int FILE_TYPE_DOCUMENT = 1;
    public final int FILE_TYPE_SPREADSHEET = 2;
    public final int FILE_TYPE_PRESENTATION = 3;
    public final int FILE_TYPE_ARCHIVE = 4;
    public final int FILE_TYPE_EXECUTABLE = 5;

    // 基本信息
    private Long fileId;     // 文件服务器ID
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private String fileUrl;         // 下载URL
    private String thumbnailUrl; // 缩略图URL
    private String localFilePath; // 文件在本地的路径
    private String localThumbnailPath;  // 缩略图在本地的路径


    // 图片消息
    private Integer imageWidth;        // 原始宽度
    private Integer imageHeight;       // 原始高度
    private Integer imageFormat;

    // 语音消息
    private Integer voiceDuration;     // 时长(毫秒)
    private Integer voiceCodec;       // 编码格式
    private Integer voiceBitrate;    // 比特率
    private Integer voiceSampleRate;  // 采样率

    // 视频消息
    private Long videoDuration;      // 时长(毫秒)
    private Integer videoWidth;
    private Integer videoHeight;
    private Integer videoCodec;
    private Integer videoBitrate;      // 比特率(kbps)

    // 位置消息
    private Double locationLatitude;
    private Double locationLongitude;
    private String locationTitle;
    private String locationAddress;
    private Integer locationZoom;         // 地图缩放级别

    // 其他文件消息
    private Integer fileType;


    // getters and setters


    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(Integer imageFormat) {
        this.imageFormat = imageFormat;
    }

    public Integer getVoiceDuration() {
        return voiceDuration;
    }

    public void setVoiceDuration(Integer voiceDuration) {
        this.voiceDuration = voiceDuration;
    }

    public Integer getVoiceCodec() {
        return voiceCodec;
    }

    public void setVoiceCodec(Integer voiceCodec) {
        this.voiceCodec = voiceCodec;
    }

    public Integer getVoiceSampleRate() {
        return voiceSampleRate;
    }

    public void setVoiceSampleRate(Integer voiceSampleRate) {
        this.voiceSampleRate = voiceSampleRate;
    }

    public Long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(Integer videoWidth) {
        this.videoWidth = videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(Integer videoHeight) {
        this.videoHeight = videoHeight;
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

    public Double getLocationLatitude() {
        return locationLatitude;
    }

    public void setLocationLatitude(Double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    public Double getLocationLongitude() {
        return locationLongitude;
    }

    public void setLocationLongitude(Double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    public String getLocationTitle() {
        return locationTitle;
    }

    public void setLocationTitle(String locationTitle) {
        this.locationTitle = locationTitle;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public Integer getLocationZoom() {
        return locationZoom;
    }

    public void setLocationZoom(Integer locationZoom) {
        this.locationZoom = locationZoom;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getLocalThumbnailPath() {
        return localThumbnailPath;
    }

    public void setLocalThumbnailPath(String localThumbnailPath) {
        this.localThumbnailPath = localThumbnailPath;
    }

    public Integer getVoiceBitrate() {
        return voiceBitrate;
    }

    public void setVoiceBitrate(Integer voiceBitrate) {
        this.voiceBitrate = voiceBitrate;
    }
}
