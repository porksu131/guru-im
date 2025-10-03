package com.guru.im.offline.service.sync;

import com.guru.im.offline.model.pojo.SyncSession;

import java.util.List;
import java.util.Map;

public interface SyncSessionService {
    SyncSession createSyncSession(SyncSession syncSession);

    SyncSession getSyncSession(Long syncId);

    List<SyncSession> getActiveSessions(Long userId, String deviceId);

    void updateSyncStatus(Long syncId, Integer syncStatus, String errorMessage);

    void updateSyncProgress(Long syncId, Integer syncedCount, Integer currentBatch,
                            Long lastSyncedSeq);

    void completeSyncSession(Long syncId, int totalSynced);

    void cleanupExpiredSessions();

    Map<String, Object> getSyncStats(Long syncId);

    boolean isSyncInProgress(Long userId, String deviceId);

    void interruptSyncSession(Long syncId, String reason);
}
