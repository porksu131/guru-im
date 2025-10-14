package com.guru.im.signal.model.pojo;

public class MediaRoomMapping {
    private Long id;
    private Long sessionId;
    private String roomId;
    private Integer workerPid;
    private String routerId;
    private Integer activePeers;
    private Integer roomStatus;
    private Long createTime;
    private Long updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Integer getWorkerPid() {
        return workerPid;
    }

    public void setWorkerPid(Integer workerPid) {
        this.workerPid = workerPid;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public Integer getActivePeers() {
        return activePeers;
    }

    public void setActivePeers(Integer activePeers) {
        this.activePeers = activePeers;
    }

    public Integer getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(Integer roomStatus) {
        this.roomStatus = roomStatus;
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