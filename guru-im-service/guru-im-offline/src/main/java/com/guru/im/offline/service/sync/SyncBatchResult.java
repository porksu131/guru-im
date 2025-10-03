package com.guru.im.offline.service.sync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncBatchResult {
    private List<byte[]> messages;
    private Map<Long, Long> serverCursorMap;
    private Long maxSeq;
    private boolean hasMore;
    private Long syncId;
    private int batchSize = 0;
    private int currentBatch = 1;
    private int totalSynced = 0;
    private int totalCount = 0;
    private int totalBatches = 0;
    private Map<Long, Long> currentCursorMap;
    private Map<Long, Boolean> convHasMoreMap;

    private boolean isFullSync = false;
    private Map<Long, Integer> conversationSyncCounts = new HashMap<>(); // 各会话计划同步数量
    private Map<Long, Integer> conversationSyncedCounts = new HashMap<>(); // 各会话已同步数量

    public SyncBatchResult() {
    }

    public SyncBatchResult(List<byte[]> messages, Map<Long, Long> serverCursorMap, Long maxSeq, boolean hasMore) {
        this.messages = messages;
        this.serverCursorMap = serverCursorMap;
        this.maxSeq = maxSeq;
        this.hasMore = hasMore;
    }

    public List<byte[]> getMessages() {
        return messages;
    }

    public void setMessages(List<byte[]> messages) {
        this.messages = messages;
    }

    public Map<Long, Long> getServerCursorMap() {
        return serverCursorMap;
    }

    public void setServerCursorMap(Map<Long, Long> serverCursorMap) {
        this.serverCursorMap = serverCursorMap;
    }

    public Long getMaxSeq() {
        return maxSeq;
    }

    public void setMaxSeq(Long maxSeq) {
        this.maxSeq = maxSeq;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Long getSyncId() {
        return syncId;
    }

    public void setSyncId(Long syncId) {
        this.syncId = syncId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getCurrentBatch() {
        return currentBatch;
    }

    public void setCurrentBatch(int currentBatch) {
        this.currentBatch = currentBatch;
    }

    public int getTotalSynced() {
        return totalSynced;
    }

    public void setTotalSynced(int totalSynced) {
        this.totalSynced = totalSynced;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(int totalBatches) {
        this.totalBatches = totalBatches;
    }

    public Map<Long, Long> getCurrentCursorMap() {
        return currentCursorMap;
    }

    public void setCurrentCursorMap(Map<Long, Long> currentCursorMap) {
        this.currentCursorMap = currentCursorMap;
    }

    public Map<Long, Boolean> getConvHasMoreMap() {
        return convHasMoreMap;
    }

    public void setConvHasMoreMap(Map<Long, Boolean> convHasMoreMap) {
        this.convHasMoreMap = convHasMoreMap;
    }


    public boolean isFullSync() {
        return isFullSync;
    }

    public void setFullSync(boolean fullSync) {
        isFullSync = fullSync;
    }

    public Map<Long, Integer> getConversationSyncCounts() {
        return conversationSyncCounts;
    }

    public void setConversationSyncCounts(Map<Long, Integer> conversationSyncCounts) {
        this.conversationSyncCounts = conversationSyncCounts;
    }

    public Map<Long, Integer> getConversationSyncedCounts() {
        return conversationSyncedCounts;
    }

    public void setConversationSyncedCounts(Map<Long, Integer> conversationSyncedCounts) {
        this.conversationSyncedCounts = conversationSyncedCounts;
    }
}