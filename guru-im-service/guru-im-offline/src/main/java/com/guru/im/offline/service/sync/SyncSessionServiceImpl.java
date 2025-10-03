package com.guru.im.offline.service.sync;

import com.guru.im.offline.mapper.SyncSessionMapper;
import com.guru.im.offline.model.pojo.SyncSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SyncSessionServiceImpl implements SyncSessionService {

    private static final Logger log = LoggerFactory.getLogger(SyncSessionServiceImpl.class);
    private final SyncSessionMapper syncSessionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SYNC_SESSION_CACHE_KEY = "sync:session:%s";
    private static final String USER_SYNC_STATUS_KEY = "user:sync:status:%s:%s";
    private static final long CACHE_EXPIRE_HOURS = 24;
    private static final long SESSION_EXPIRE_TIME = 30 * 60 * 1000; // 30分钟

    public SyncSessionServiceImpl(SyncSessionMapper syncSessionMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.syncSessionMapper = syncSessionMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public SyncSession createSyncSession(SyncSession syncSession) {
        try {
            syncSessionMapper.insert(syncSession);

            // 更新缓存
            updateSessionCache(syncSession);
            updateUserSyncStatus(syncSession.getUserId(), syncSession.getDeviceId(), true);

            log.info("Created sync session: syncId={}, userId={}, deviceId={}",
                    syncSession.getSyncId(), syncSession.getUserId(), syncSession.getDeviceId());

            return syncSession;

        } catch (Exception e) {
            log.error("Create sync session failed", e);
            throw new RuntimeException("Create sync session failed", e);
        }
    }

    @Override
    public SyncSession getSyncSession(Long syncId) {
        String cacheKey = String.format(SYNC_SESSION_CACHE_KEY, syncId);
        SyncSession session = (SyncSession) redisTemplate.opsForValue().get(cacheKey);

        if (session == null) {
            session = syncSessionMapper.findBySyncId(syncId);
            if (session != null) {
                redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }

        return session;
    }

    @Override
    public List<SyncSession> getActiveSessions(Long userId, String deviceId) {
        return syncSessionMapper.findActiveSessionsByUserAndDevice(userId, deviceId);
    }

    @Override
    @Transactional
    public void updateSyncStatus(Long syncId, Integer syncStatus, String errorMessage) {
        Long updateTime = System.currentTimeMillis();

        try {
            syncSessionMapper.updateSyncStatus(syncId, syncStatus, errorMessage, updateTime);

            // 更新缓存
            SyncSession session = getSyncSession(syncId);
            if (session != null) {
                session.setSyncStatus(syncStatus);
                session.setErrorMessage(errorMessage);
                session.setUpdateTime(updateTime);
                updateSessionCache(session);

                // 如果同步完成，更新用户同步状态
                if (syncStatus == 3 || syncStatus >= 4) { // 已送达或失败状态
                    updateUserSyncStatus(session.getUserId(), session.getDeviceId(), false);
                }
            }

        } catch (Exception e) {
            log.error("Update sync status failed: syncId={}", syncId, e);
            throw new RuntimeException("Update sync status failed", e);
        }
    }

    @Override
    @Transactional
    public void updateSyncProgress(Long syncId, Integer syncedCount, Integer currentBatch,
                                   Long lastSyncedSeq) {
        Long updateTime = System.currentTimeMillis();
        Long lastActivityTime = System.currentTimeMillis();

        try {
            syncSessionMapper.updateSyncProgress(syncId, syncedCount, currentBatch,
                    lastSyncedSeq, lastActivityTime, updateTime);

            // 更新缓存
            SyncSession session = getSyncSession(syncId);
            if (session != null) {
                session.setSyncedCount(syncedCount);
                session.setCurrentBatch(currentBatch);
                session.setLastSyncedSeq(lastSyncedSeq);
                session.setLastActivityTime(lastActivityTime);
                session.setUpdateTime(updateTime);
                updateSessionCache(session);
            }

        } catch (Exception e) {
            log.error("Update sync progress failed: syncId={}", syncId, e);
            throw new RuntimeException("Update sync progress failed", e);
        }
    }

    @Override
    @Transactional
    public void completeSyncSession(Long syncId, int totalSynced) {
        try {
            Long updateTime = System.currentTimeMillis();
            Long endTime = System.currentTimeMillis();

            // 更新同步会话状态为已完成
            int affectedRows = syncSessionMapper.completeSyncSession(
                    syncId, 3, endTime, updateTime); // 3: 已送达

            if (affectedRows > 0) {
                // 获取会话信息以更新用户状态
                SyncSession session = getSyncSession(syncId);
                if (session != null) {
                    // 更新用户同步状态为非进行中，即同步完成
                    updateUserSyncStatus(session.getUserId(), session.getDeviceId(), false);

                    // 记录同步完成日志
                    Map<String, Object> stats = getSyncStats(syncId);
                    log.info("Sync completed: syncId={}, userId={}, totalMessages={}, duration={}s",
                            syncId, session.getUserId(), totalSynced,
                            stats != null ? stats.get("durationSeconds") : "unknown");
                }

                // 清理会话缓存
                String cacheKey = String.format(SYNC_SESSION_CACHE_KEY, syncId);
                redisTemplate.delete(cacheKey);
            } else {
                log.warn("Complete sync session affected 0 rows: syncId={}", syncId);
            }

        } catch (Exception e) {
            log.error("Complete sync session failed: syncId={}", syncId, e);
            // 即使失败也尝试更新状态
            updateSyncStatus(syncId, 5, "Complete failed: " + e.getMessage());
            throw new RuntimeException("Complete sync session failed", e);
        }
    }

    @Override
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    @Transactional
    public void cleanupExpiredSessions() {
        Long expireTime = System.currentTimeMillis() - SESSION_EXPIRE_TIME;

        try {
            List<SyncSession> expiredSessions = syncSessionMapper.findExpiredSessions(expireTime);

            for (SyncSession session : expiredSessions) {
                // 将过期会话标记为失败
                syncSessionMapper.updateSyncStatus(session.getSyncId(), 5, "Session expired",
                        System.currentTimeMillis());

                // 清理缓存
                String cacheKey = String.format(SYNC_SESSION_CACHE_KEY, session.getSyncId());
                redisTemplate.delete(cacheKey);

                // 更新用户同步状态
                updateUserSyncStatus(session.getUserId(), session.getDeviceId(), false);

                log.warn("Cleaned up expired sync session: syncId={}, userId={}",
                        session.getSyncId(), session.getUserId());
            }

        } catch (Exception e) {
            log.error("Cleanup expired sessions failed", e);
        }
    }

    @Override
    public Map<String, Object> getSyncStats(Long syncId) {
        return syncSessionMapper.findSyncStats(syncId);
    }

    @Override
    public boolean isSyncInProgress(Long userId, String deviceId) {
        String statusKey = String.format(USER_SYNC_STATUS_KEY, userId, deviceId);
        Boolean inProgress = (Boolean) redisTemplate.opsForValue().get(statusKey);
        return Boolean.TRUE.equals(inProgress);
    }

    @Override
    @Transactional
    public void interruptSyncSession(Long syncId, String reason) {
        updateSyncStatus(syncId, 5, "Interrupted: " + reason); // 送达失败
        log.info("Interrupted sync session: syncId={}, reason={}", syncId, reason);
    }

    private void updateSessionCache(SyncSession session) {
        String cacheKey = String.format(SYNC_SESSION_CACHE_KEY, session.getSyncId());
        redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void updateUserSyncStatus(Long userId, String deviceId, boolean inProgress) {
        String statusKey = String.format(USER_SYNC_STATUS_KEY, userId, deviceId);
        if (inProgress) {
            redisTemplate.opsForValue().set(statusKey, true, 1, TimeUnit.HOURS);
        } else {
            redisTemplate.delete(statusKey);
        }
    }
}