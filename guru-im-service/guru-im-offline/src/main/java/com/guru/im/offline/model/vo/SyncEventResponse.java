package com.guru.im.offline.model.vo;

import com.guru.im.protocol.model.Event;
import com.guru.im.protocol.model.SyncStatus;

import java.util.List;

public class SyncEventResponse {
    private Long syncEventId;
    private Long latestSequence;
    private List<Event> events;
    private Boolean hasMore;
    private SyncStatus syncStatus;
    private String errorMsg;

    public Long getSyncEventId() {
        return syncEventId;
    }

    public void setSyncEventId(Long syncEventId) {
        this.syncEventId = syncEventId;
    }

    public Long getLatestSequence() {
        return latestSequence;
    }

    public void setLatestSequence(Long latestSequence) {
        this.latestSequence = latestSequence;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}