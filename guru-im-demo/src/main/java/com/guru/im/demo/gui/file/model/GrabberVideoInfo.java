package com.guru.im.demo.gui.file.model;

import java.awt.image.BufferedImage;

public class GrabberVideoInfo {
    private Long duration;
    private Integer audioCodec;
    private Integer audioBitrate;
    private Integer videoCodec;
    private Integer videoBitrate;
    private Integer imageWidth;
    private Integer imageHeight;
    private BufferedImage screenshot;


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

    public BufferedImage getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(BufferedImage screenshot) {
        this.screenshot = screenshot;
    }
}
