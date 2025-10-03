package com.guru.im.offline.service.sync;


import com.guru.im.offline.model.pojo.SyncSession;
import com.guru.im.offline.service.OfflineMessageService;
import com.guru.im.protocol.model.OfflineSyncAck;
import com.guru.im.protocol.model.OfflineSyncStatus;
import com.guru.im.protocol.model.OfflineSyncType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class SyncAckService {
    private static final Logger log = LoggerFactory.getLogger(SyncAckService.class);
    private final OfflineMessageService offlineMessageService;
    private final SyncCursorService syncCursorService;
    private final SyncSessionService syncSessionService;
    private final RedisTemplate<String, Object> redisTemplate;

    public SyncAckService(OfflineMessageService offlineMessageService,
                          SyncCursorService syncCursorService,
                          SyncSessionService syncSessionService,
                          RedisTemplate<String, Object> redisTemplate) {
        this.offlineMessageService = offlineMessageService;
        this.syncCursorService = syncCursorService;
        this.syncSessionService = syncSessionService;
        this.redisTemplate = redisTemplate;
    }

    public void processMessage(OfflineSyncAck offlineSyncAck) {
        try {
            if (offlineSyncAck.getBatchNumber() > 0) {
                // 标记批次已ACK
                markBatchAcknowledged(offlineSyncAck.getSyncId(), offlineSyncAck.getBatchNumber());
            }

            if (offlineSyncAck.getSuccess()) {
                // 处理成功的ACK
                if (offlineSyncAck.getSyncType() == OfflineSyncType.SYNC_TYPE_FULL) {
                    // 全量同步
                    handleFullSyncSuccessfulAck(offlineSyncAck);
                } else {
                    // 增量同步
                    handleSuccessfulAck(offlineSyncAck);
                }


            } else {
                // 处理失败的ACK
                handleFailedAck(offlineSyncAck);
            }

            if (offlineSyncAck.getFinal()) {
                completeSyncWithCleanup(offlineSyncAck);
            }

        } catch (Exception e) {
            log.error("Process sync ack failed", e);
        }
    }

    private void handleSuccessfulAck(OfflineSyncAck offlineSyncAck) {
        Long userId = offlineSyncAck.getUserId();
        String deviceId = offlineSyncAck.getDeviceId();
        Long syncId = offlineSyncAck.getSyncId();
        try {
            // 获取确认的消息ID列表
            List<Long> receivedMsgIds = offlineSyncAck.getReceivedMsgIdsList();

            // 获取确认的游标信息
            Map<Long, Long> confirmedCursorMap = offlineSyncAck.getConfirmedCursorMapMap();

            // 1. 处理离线消息确认
            if (!receivedMsgIds.isEmpty()) {
                offlineMessageService.processSyncAck(userId, deviceId, receivedMsgIds);
            }

            // 2. 更新同步位点信息（关键步骤！）
            if (!confirmedCursorMap.isEmpty()) {
                syncCursorService.batchUpdateSyncCursors(userId, deviceId, confirmedCursorMap);
                log.info("Updated sync cursors for userId: {}, deviceId: {}, cursorCount: {}",
                        userId, deviceId, confirmedCursorMap.size());
            }

            // 3. 完成同步会话
            syncSessionService.completeSyncSession(syncId, OfflineSyncStatus.OFFLINE_SYNC_STATUS_DELIVERED_VALUE); // 3:已送达

            // 4. 记录详细的ACK信息
            Integer receivedCount = offlineSyncAck.getReceivedCount();
            Integer batchNumber = offlineSyncAck.getBatchNumber();

            log.info("Sync ack processed successfully: syncId={}, userId={}, deviceId={}, receivedCount={}, batchNumber={}",
                    syncId, userId, deviceId, receivedCount, batchNumber);

        } catch (Exception e) {
            log.error("Handle successful sync ack failed: syncId={}", syncId, e);
            // 标记同步会话为失败状态
            syncSessionService.updateSyncStatus(syncId, 5, "ACK处理失败: " + e.getMessage());
        }
    }

    private void handleFailedAck(OfflineSyncAck offlineSyncAck) {
        Long userId = offlineSyncAck.getUserId();
        String deviceId = offlineSyncAck.getDeviceId();
        Long syncId = offlineSyncAck.getSyncId();
        String errorMsg = offlineSyncAck.getErrorMsg();
        Integer errorCode = offlineSyncAck.getErrorCode();
        log.warn("Sync ack failed: syncId={}, userId={}, deviceId={}, errorCode={}, error={}",
                syncId, userId, deviceId, errorCode, errorMsg);

        // 更新同步会话状态为失败
        syncSessionService.updateSyncStatus(syncId, 5, "ACK失败: " + errorMsg);

        // 根据错误码决定是否重试
        if (shouldRetry(errorCode)) {
            scheduleRetry(syncId, userId, deviceId);
        }
    }

    private boolean shouldRetry(Integer errorCode) {
        // 可重试的错误码：网络超时、临时错误等
        return errorCode != null && errorCode >= 1000 && errorCode < 2000;
    }

    private void scheduleRetry(Long syncId, Long userId, String deviceId) {
        // 实现重试逻辑
        log.info("Scheduling retry for failed sync: syncId={}, userId={}, deviceId={}",
                syncId, userId, deviceId);

        // 这里可以发送重试消息到MQ或者使用定时任务
    }


    /**
     * 完成同步并清理资源
     */
    private void completeSyncWithCleanup(OfflineSyncAck offlineSyncAck) {
        try {
            // 完成同步会话
            syncSessionService.completeSyncSession(offlineSyncAck.getSyncId(), 3);

            // 清理所有相关Redis状态
            cleanupAllSyncState(offlineSyncAck.getSyncId());

            // 清理游标数据
            cleanupCursors(offlineSyncAck.getSyncId());

            log.info("Sync completed with cleanup: syncId={}, userId={}", offlineSyncAck.getSyncId(), offlineSyncAck.getUserId());

        } catch (Exception e) {
            log.error("Complete sync with cleanup failed: syncId={}", offlineSyncAck.getSyncId(), e);
        }
    }

    /**
     * 清理所有同步相关的Redis状态
     */
    private void cleanupAllSyncState(Long syncId) {
        try {
            // 清理所有批次的ACK状态
            cleanupAllBatchStates(syncId);

            // 清理其他同步相关状态
            Set<String> keysToDelete = redisTemplate.keys("sync:*:" + syncId + "*");
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }

        } catch (Exception e) {
            log.warn("Cleanup all sync state failed: syncId={}", syncId, e);
        }
    }

    /**
     * 清理所有批次的ACK状态
     */
    private void cleanupAllBatchStates(Long syncId) {
        try {
            // 清理所有可能的批次状态
            for (int i = 1; i <= 1000; i++) {
                cleanupBatchAckState(syncId, i);
            }
        } catch (Exception e) {
            log.warn("Cleanup all batch states failed: syncId={}", syncId, e);
        }
    }


    // 添加定时任务检测ACK超时
    @Scheduled(fixedRate = 10000) // 每10秒检测一次
    public void checkAckTimeouts() {
        try {
            Set<String> waitingKeys = redisTemplate.keys("sync:waiting:*");
            if (waitingKeys != null) {
                for (String waitingKey : waitingKeys) {
                    // 解析syncId和batchNumber
                    String[] parts = waitingKey.split(":");
                    if (parts.length >= 4) {
                        Long syncId = Long.parseLong(parts[2]);
                        int batchNumber = Integer.parseInt(parts[3]);

                        // 检查是否超时
                        if (isBatchTimeout(syncId, batchNumber)) {
                            log.warn("Batch ACK timeout: syncId={}, batch={}", syncId, batchNumber);
                            handleAckTimeout(syncId, batchNumber);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Check ACK timeouts failed", e);
        }
    }

    /**
     * 处理ACK超时
     */
    private void handleAckTimeout(Long syncId, int batchNumber) {
        try {
            // 清理ACK状态
            cleanupBatchAckState(syncId, batchNumber);

            // 更新同步状态为失败
            syncSessionService.updateSyncStatus(syncId, 5,
                    "ACK timeout for batch " + batchNumber);

            // 可以根据策略决定是否重试
            if (shouldRetryOnTimeout(syncId)) {
                //scheduleRetry(syncId, batchNumber);
            }

        } catch (Exception e) {
            log.error("Handle ACK timeout failed: syncId={}, batch={}", syncId, batchNumber, e);
        }
    }

    /**
     * 判断是否应该重试
     */
    private boolean shouldRetryOnTimeout(Long syncId) {
        // 根据同步会话的状态和重试次数决定
        SyncSession session = syncSessionService.getSyncSession(syncId);
        return session != null && session.getSyncStatus() == 1; // 只有进行中的会话才重试
    }


    /**
     * 清理游标数据
     */
    public void cleanupCursors(Long syncId) {
        String key = getSyncCursorKey(syncId);
        redisTemplate.delete(key);
    }

    /**
     * 检查批次是否超时
     */
    public boolean isBatchTimeout(Long syncId, int batchNumber) {
        String sendTimeKey = getBatchSendTimeKey(syncId, batchNumber);
        Long sendTime = (Long) redisTemplate.opsForValue().get(sendTimeKey);

        if (sendTime != null) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - sendTime;
            return elapsed > 30000; // 30秒超时
        }

        return false;
    }

    /**
     * 检查该批次是否已被ACK
     */
    public boolean isBatchAcknowledged(Long syncId, int batchNumber) {
        String ackKey = getBatchAckKey(syncId, batchNumber);
        return Boolean.TRUE.equals(redisTemplate.hasKey(ackKey));
    }

    /**
     * 设置批次等待ACK状态
     */
    public void setBatchWaitingForAck(Long syncId, int batchNumber) {
        String waitingKey = getBatchWaitingKey(syncId, batchNumber);
        redisTemplate.opsForValue().set(waitingKey, "waiting", 5, TimeUnit.MINUTES);

        // 记录批次发送时间，用于超时检测
        String sendTimeKey = getBatchSendTimeKey(syncId, batchNumber);
        redisTemplate.opsForValue().set(sendTimeKey, System.currentTimeMillis(), 5, TimeUnit.MINUTES);

        log.debug("Set batch waiting for ACK: syncId={}, batch={}", syncId, batchNumber);
    }

    /**
     * 清理批次ACK状态
     */
    public void cleanupBatchAckState(Long syncId, int batchNumber) {
        try {
            // 清理等待状态
            String waitingKey = getBatchWaitingKey(syncId, batchNumber);
            redisTemplate.delete(waitingKey);

            // 清理发送时间
            String sendTimeKey = getBatchSendTimeKey(syncId, batchNumber);
            redisTemplate.delete(sendTimeKey);

            // 清理ACK标记
            String ackKey = getBatchAckKey(syncId, batchNumber);
            redisTemplate.delete(ackKey);

            log.debug("Cleaned up batch ACK state: syncId={}, batch={}", syncId, batchNumber);

        } catch (Exception e) {
            log.warn("Cleanup batch ACK state failed: syncId={}, batch={}", syncId, batchNumber, e);
        }
    }

    /**
     * 标记批次已ACK
     */
    public void markBatchAcknowledged(Long syncId, int batchNumber) {
        String ackKey = getBatchAckKey(syncId, batchNumber);
        redisTemplate.opsForValue().set(ackKey, true, 5, TimeUnit.MINUTES);

        // 清理等待状态
        String waitingKey = getBatchWaitingKey(syncId, batchNumber);
        redisTemplate.delete(waitingKey);

        log.debug("Batch acknowledged: syncId={}, batch={}", syncId, batchNumber);
    }

    // Redis key生成方法
    private String getBatchWaitingKey(Long syncId, int batchNumber) {
        return String.format("sync:waiting:%s:%d", syncId, batchNumber);
    }

    private String getBatchAckKey(Long syncId, int batchNumber) {
        return String.format("sync:ack:%s:%d", syncId, batchNumber);
    }

    private String getBatchSendTimeKey(Long syncId, int batchNumber) {
        return String.format("sync:sendtime:%s:%d", syncId, batchNumber);
    }

    private String getSyncCursorKey(Long syncId) {
        return String.format("sync:cursors:%s", syncId);
    }


    /**
     * 处理全量同步成功ACK
     */
    private void handleFullSyncSuccessfulAck(OfflineSyncAck offlineSyncAck) {
        Long userId = offlineSyncAck.getUserId();
        String deviceId = offlineSyncAck.getDeviceId();
        Long syncId = offlineSyncAck.getSyncId();

        try {
            // 1. 处理消息确认（复用现有逻辑）
            List<Long> receivedMsgIds = offlineSyncAck.getReceivedMsgIdsList();
            if (!receivedMsgIds.isEmpty()) {
                offlineMessageService.processSyncAck(userId, deviceId, receivedMsgIds);
            }

            // 2. 更新同步位点（全量同步使用客户端确认的最新位点）
            Map<Long, Long> confirmedCursorMap = offlineSyncAck.getConfirmedCursorMapMap();
            if (!confirmedCursorMap.isEmpty()) {
                syncCursorService.batchUpdateSyncCursors(userId, deviceId, confirmedCursorMap);
                log.info("Updated full sync cursors for userId: {}, deviceId: {}, cursorCount: {}",
                        userId, deviceId, confirmedCursorMap.size());
            }

            // 3. 完成同步会话
            syncSessionService.completeSyncSession(syncId,
                    OfflineSyncStatus.OFFLINE_SYNC_STATUS_DELIVERED_VALUE);

            log.info("Full sync ack processed: syncId={}, userId={}, deviceId={}, batchNumber={}",
                    syncId, userId, deviceId, offlineSyncAck.getBatchNumber());

        } catch (Exception e) {
            log.error("Handle full sync ack failed: syncId={}", syncId, e);
            syncSessionService.updateSyncStatus(syncId, 5, "Full sync ACK处理失败: " + e.getMessage());
        }
    }

}
