package com.guru.im.signal.model.pojo;

public class CallParticipant {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String deviceId;
    private Integer participantState; // 0-7 对应ParticipantState
    private Integer isInviter;
    private Long joinTime;
    private Long leaveTime;
    private Integer duration;
    private String mediaState; // JSON格式
    private String selectedDevice;
    private Long createTime;
    private Long updateTime;


    // 参与者状态枚举
    public enum ParticipantState {
        INVITED(0), RINGING(1), JOINED(2), DECLINED(3),
        LEFT(4), TIMEOUT(5), KICKED(6), RECONNECTING(7);

        private final int value;
        ParticipantState(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public Integer getParticipantState() {
        return participantState;
    }

    public void setParticipantState(Integer participantState) {
        this.participantState = participantState;
    }

    public Integer getIsInviter() {
        return isInviter;
    }

    public void setIsInviter(Integer isInviter) {
        this.isInviter = isInviter;
    }

    public Long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Long joinTime) {
        this.joinTime = joinTime;
    }

    public Long getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(Long leaveTime) {
        this.leaveTime = leaveTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getMediaState() {
        return mediaState;
    }

    public void setMediaState(String mediaState) {
        this.mediaState = mediaState;
    }

    public String getSelectedDevice() {
        return selectedDevice;
    }

    public void setSelectedDevice(String selectedDevice) {
        this.selectedDevice = selectedDevice;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}