package com.guru.im.signal.model.pojo;

public class CallSession {
    private Long id;
    private Integer sessionType; // 1:私聊通话 2:群组通话 3:会议
    private Integer mediaType;   // 0:仅音频 1:视频 2:屏幕共享 3:群组视频
    private Long initiatorId;
    private String initiatorDevice;
    private Long groupId;
    private Long conferenceId;
    private String conferenceTitle;
    private String callSubject;
    private Integer callState;   // 0-6 对应CallState
    private Integer maxParticipants;
    private Integer timeoutSeconds;
    private Long startTime;
    private Long endTime;
    private Integer duration;
    private String hangupReason;
    private Integer hangupType;
    private Long hangupInitiator;
    private Long createTime;
    private Long updateTime;


    // 通话状态枚举
    public enum CallState {
        IDLE(0), DIALING(1), RINGING(2), CONNECTING(3),
        ACTIVE(4), HANGING_UP(5), ENDED(6);

        private final int value;
        CallState(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    // 会话类型枚举
    public enum SessionType {
        PRIVATE(1), GROUP(2), CONFERENCE(3);
        private final int value;
        SessionType(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    // 媒体类型枚举
    public enum MediaType {
        AUDIO_ONLY(0), VIDEO(1), SCREEN_SHARE(2), GROUP_VIDEO(3);
        private final int value;
        MediaType(int value) { this.value = value; }
        public int getValue() { return value; }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSessionType() {
        return sessionType;
    }

    public void setSessionType(Integer sessionType) {
        this.sessionType = sessionType;
    }

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getInitiatorDevice() {
        return initiatorDevice;
    }

    public void setInitiatorDevice(String initiatorDevice) {
        this.initiatorDevice = initiatorDevice;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(Long conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getConferenceTitle() {
        return conferenceTitle;
    }

    public void setConferenceTitle(String conferenceTitle) {
        this.conferenceTitle = conferenceTitle;
    }

    public String getCallSubject() {
        return callSubject;
    }

    public void setCallSubject(String callSubject) {
        this.callSubject = callSubject;
    }

    public Integer getCallState() {
        return callState;
    }

    public void setCallState(Integer callState) {
        this.callState = callState;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getHangupReason() {
        return hangupReason;
    }

    public void setHangupReason(String hangupReason) {
        this.hangupReason = hangupReason;
    }

    public Integer getHangupType() {
        return hangupType;
    }

    public void setHangupType(Integer hangupType) {
        this.hangupType = hangupType;
    }

    public Long getHangupInitiator() {
        return hangupInitiator;
    }

    public void setHangupInitiator(Long hangupInitiator) {
        this.hangupInitiator = hangupInitiator;
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