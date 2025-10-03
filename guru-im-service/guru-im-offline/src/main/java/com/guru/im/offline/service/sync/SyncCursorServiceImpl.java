package com.guru.im.offline.service.sync;

import com.guru.im.offline.mapper.SyncCursorMapper;
import com.guru.im.offline.model.pojo.SyncCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SyncCursorServiceImpl implements SyncCursorService {

    private static final Logger log = LoggerFactory.getLogger(SyncCursorServiceImpl.class);
    private final SyncCursorMapper syncCursorMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SYNC_CURSOR_CACHE_KEY = "sync:cursor:%s:%s";

    public SyncCursorServiceImpl(SyncCursorMapper syncCursorMapper,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.syncCursorMapper = syncCursorMapper;
        this.redisTemplate = redisTemplate;
    }
    @Override
    @Transactional
    public void batchUpdateSyncCursors(Long userId, String deviceId, Map<Long, Long> cursorMap) {
        if (cursorMap == null || cursorMap.isEmpty()) return;

        Long updateTime = System.currentTimeMillis();
        for (Map.Entry<Long, Long> entry : cursorMap.entrySet()) {
            Integer conversationType = 1; // 默认单聊
            updateSyncCursor(userId, deviceId, conversationType, entry.getKey(), entry.getValue());
        }
        log.info("Updated {} sync cursors for userId: {}", cursorMap.size(), userId);
    }

    @Override
    @Transactional
    public void updateSyncCursor(Long userId, String deviceId, Integer conversationType,
                                 Long conversationId, Long lastSyncSeq) {
        Long updateTime = System.currentTimeMillis();
        syncCursorMapper.updateLastSyncSeq(userId, deviceId, conversationType, conversationId,
                lastSyncSeq, updateTime);
        updateCursorCache(userId, deviceId, conversationType, conversationId, lastSyncSeq, updateTime);
    }

    @Override
    public Map<Long, Long> getClientCursorMap(Long userId, String deviceId) {
        Map<Long, Long> cursorMap = new HashMap<>();
        List<SyncCursor> cursors = syncCursorMapper.findByUserAndDevice(userId, deviceId);
        for (SyncCursor cursor : cursors) {
            cursorMap.put(cursor.getConversationId(), cursor.getLastSyncSeq());
        }
        return cursorMap;
    }

    @Override
    public boolean checkHasMoreMessages(Long userId, Map<Long, Long> clientCursorMap) {
        for (Map.Entry<Long, Long> entry : clientCursorMap.entrySet()) {
            Integer count = syncCursorMapper.countNewMessages(userId, entry.getKey(), entry.getValue());
            if (count != null && count > 0) return true;
        }
        return false;
    }

    private void updateCursorCache(Long userId, String deviceId, Integer conversationType,
                                   Long conversationId, Long lastSyncSeq, Long updateTime) {
        String cacheKey = String.format(SYNC_CURSOR_CACHE_KEY, userId, deviceId);
        String hashKey = conversationType + ":" + conversationId;
        redisTemplate.opsForHash().put(cacheKey, hashKey, lastSyncSeq);
        redisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);
    }
}
