package com.guru.im.offline.service.sync;

import java.util.Map;

public interface SyncCursorService {
    void updateSyncCursor(Long userId, String deviceId,
                          Integer conversationType, Long conversationId,
                          Long lastSyncSeq);

    Map<Long, Long> getClientCursorMap(Long userId, String deviceId);

    void batchUpdateSyncCursors(Long userId, String deviceId, Map<Long, Long> cursorMap);

    boolean checkHasMoreMessages(Long userId, Map<Long, Long> clientCursorMap);
}