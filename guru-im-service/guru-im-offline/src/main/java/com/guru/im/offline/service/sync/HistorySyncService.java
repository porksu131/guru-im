package com.guru.im.offline.service.sync;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.model.pojo.Message;
import com.guru.im.offline.model.pojo.SyncSession;
import com.guru.im.offline.service.ConversationService;
import com.guru.im.offline.service.MessageConverter;
import com.guru.im.offline.service.MessageService;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.OfflineSyncRequest;
import com.guru.im.protocol.model.OfflineSyncResponse;
import com.guru.im.protocol.model.SyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HistorySyncService {

    private static final Logger log = LoggerFactory.getLogger(HistorySyncService.class);
    private final SyncSessionService syncSessionService;
    private final SnowflakeIdGenerator idGenerator;
    private final SyncAckService syncAckService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CommonSyncService commonSyncService;
    // 全量同步相关常量
    private static final int FULL_SYNC_MAX_MESSAGES = 1000; // 全量同步最大消息数
    private static final int FULL_SYNC_BATCH_SIZE = 50;     // 全量同步批次大小
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 500;

    public HistorySyncService(SyncSessionService syncSessionService,
                              SnowflakeIdGenerator idGenerator,
                              SyncAckService syncAckService,
                              ConversationService conversationService, MessageService messageService, CommonSyncService commonSyncService) {

        this.syncSessionService = syncSessionService;
        this.idGenerator = idGenerator;
        this.syncAckService = syncAckService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.commonSyncService = commonSyncService;
    }


    /**
     * 处理全量同步请求
     */
    public void handleFullSyncRequest(OfflineSyncRequest request) {
        // 1. 检查是否正在同步
        if (syncSessionService.isSyncInProgress(request.getUserId(), request.getDeviceId())) {
            commonSyncService.sendSyncErrorResponse(request, "Sync already in progress");
            return;
        }

        // 2. 创建全量同步会话
        SyncSession syncSession = createFullSyncSession(request);

        // 3. 计算各会话的同步数量
        Map<Long, Integer> syncCountsPerConversation = calculateFullSyncCounts(request);

        // 4. 执行全量同步
        processFullSyncInBatches(request, syncSession, syncCountsPerConversation);
    }

    /**
     * 创建全量同步会话
     */
    private SyncSession createFullSyncSession(OfflineSyncRequest request) {
        SyncSession session = new SyncSession();
        session.setId(idGenerator.nextId());
        session.setSyncId(request.getSyncId() > 0 ? request.getSyncId() : idGenerator.nextId());
        session.setUserId(request.getUserId());
        session.setDeviceId(request.getDeviceId());
        session.setSyncType(request.getSyncTypeValue());
        session.setBatchSize(getFullSyncBatchSize(request));
        session.setSyncStatus(SyncStatus.SYNC_STATUS_IN_PROGRESS_VALUE);
        session.setStartTime(System.currentTimeMillis());
        session.setCreateTime(System.currentTimeMillis());
        session.setUpdateTime(System.currentTimeMillis());
        session.setLastActivityTime(System.currentTimeMillis());

        // 计算总消息数（用于进度跟踪）
        int totalCount = calculateTotalFullSyncCount(request);
        session.setTotalCount(totalCount);

        return syncSessionService.createSyncSession(session);
    }

    /**
     * 计算全量同步各会话的消息数量
     */
    private Map<Long, Integer> calculateFullSyncCounts(OfflineSyncRequest request) {
        Map<Long, Integer> syncCounts = new HashMap<>();
        Map<Long, Integer> unreadCounts = conversationService.getUnreadCounts(request.getUserId());

        for (Map.Entry<Long, Long> entry : request.getClientCursorMapMap().entrySet()) {
            Long conversationId = entry.getKey();
            Long latestSeq = entry.getValue(); // 客户端传来的最新序列号

            // 查询该会话的总消息数量（从主消息表）
            Integer totalCount = messageService.countMessages(null, conversationId, latestSeq);
            Integer unreadCount = unreadCounts.getOrDefault(conversationId, 0);

            int syncCount;
            if (totalCount != null && totalCount > 0) {
                if (unreadCount <= 99) {
                    // 未读数≤99：同步未读数+20，但不超过总会话数
                    syncCount = Math.min(unreadCount + 20, totalCount);
                } else {
                    // 未读数>99：只同步最近20条
                    syncCount = Math.min(20, totalCount);
                }
            } else {
                syncCount = 0; // 没有消息
            }

            // 应用全局限制
            syncCount = Math.min(syncCount, 100); // 单个会话最多同步100条
            syncCounts.put(conversationId, syncCount);
        }

        return syncCounts;
    }

    /**
     * 全量同步分批处理
     */
    private void processFullSyncInBatches(OfflineSyncRequest request,
                                          SyncSession syncSession,
                                          Map<Long, Integer> syncCounts) {
        Long syncId = syncSession.getSyncId();
        int batchSize = getFullSyncBatchSize(request);

        try {
            SyncBatchResult batch = new SyncBatchResult();
            batch.setSyncId(syncId);
            batch.setBatchSize(batchSize);
            batch.setTotalCount(calculateTotalFullSyncCount(request));
            batch.setTotalBatches((int) Math.ceil((double) batch.getTotalCount() / batchSize));
            batch.setCurrentCursorMap(request.getClientCursorMapMap());
            batch.setCurrentBatch(0);
            batch.setFullSync(true);
            batch.setConversationSyncCounts(syncCounts);

            // 分批获取和发送消息
            while (batch.getTotalSynced() < batch.getTotalCount()) {
                // 获取下一批全量同步消息
                getNextFullSyncBatch(request, batch, syncCounts);

                if (batch.getMessages().isEmpty()) {
                    log.info("No more messages in full sync: syncId={}", syncId);
                    break;
                }

                // 发送当前批次
                ResponseResult<Void> sendResponse = sendFullSyncBatch(request, batch);
                if (ResponseResult.isError(sendResponse)) {
                    syncAckService.cleanupBatchAckState(syncId, batch.getCurrentBatch());
                    throw new RuntimeException(sendResponse.getMsg());
                }

                // 设置等待ACK状态
                syncAckService.setBatchWaitingForAck(syncId, batch.getCurrentBatch());

                batch.setTotalSynced(batch.getTotalSynced() + batch.getMessages().size());

                // 更新同步进度
                syncSessionService.updateSyncProgress(syncId, batch.getTotalSynced(),
                        batch.getCurrentBatch(), batch.getMaxSeq());

                log.debug("Sent full sync batch {}: syncId={}, messages={}, totalSynced={}",
                        batch.getCurrentBatch(), syncId, batch.getMessages().size(), batch.getTotalSynced());

                // 等待ACK（复用现有逻辑）
                if (batch.getTotalSynced() < batch.getTotalCount()) {
                    commonSyncService.waitForAck(syncId, batch.getCurrentBatch());
                    batch.setCurrentBatch(batch.getCurrentBatch() + 1);
                } else {
                    break;
                }

                // 安全限制
                if (batch.getCurrentBatch() > 100) {
                    log.warn("Full sync batch limit reached: syncId={}", syncId);
                    break;
                }
            }

            // 完成全量同步
            completeFullSync(syncSession, batch, syncCounts);

        } catch (Exception e) {
            log.error("Full sync batches failed: syncId={}", syncId, e);
            commonSyncService.sendSyncErrorResponse(request, e.getMessage());
            syncSessionService.updateSyncStatus(syncId, 5, "Full sync failed: " + e.getMessage());
        }
    }


    /**
     * 获取下一批全量同步消息
     */
    private void getNextFullSyncBatch(OfflineSyncRequest request,
                                      SyncBatchResult batchResult,
                                      Map<Long, Integer> syncCounts) {
        List<byte[]> messages = new ArrayList<>();
        Map<Long, Long> serverCursorMap = new HashMap<>();
        long maxSeq = 0L;
        int messagesInBatch = 0;

        // 遍历所有需要同步的会话
        for (Map.Entry<Long, Integer> entry : syncCounts.entrySet()) {
            Long conversationId = entry.getKey();
            Integer targetCount = entry.getValue();

            if (messagesInBatch >= batchResult.getBatchSize()) {
                break;
            }

            // 计算这个会话在当前批次中还能获取多少条消息
            int remainingInBatch = batchResult.getBatchSize() - messagesInBatch;
            int neededForConversation = targetCount -
                    batchResult.getConversationSyncedCounts().getOrDefault(conversationId, 0);

            if (neededForConversation <= 0) {
                continue; // 这个会话已经同步完成
            }

            int limit = Math.min(remainingInBatch, neededForConversation);

            // 获取这个会话的消息（从主消息表im_message）
            List<Message> conversationMessages = getMessagesForFullSync(
                    request.getUserId(), conversationId,
                    request.getClientCursorMapMap().get(conversationId), limit);

            if (!conversationMessages.isEmpty()) {
                conversationMessages.sort(Comparator.comparing(Message::getServerSeq));
                // 转换消息格式为客户端需要的格式
                for (Message msg : conversationMessages) {
                    try {
                        // 将MessageContent转换为ImMessage格式
                        byte[] messageBytes = convertToImMessageBytes(msg);
                        messages.add(messageBytes);
                        messagesInBatch++;
                        maxSeq = Math.max(maxSeq, msg.getServerSeq());

                        if (messagesInBatch >= batchResult.getBatchSize()) break;
                    } catch (Exception e) {
                        log.warn("Convert message failed, skip: messageId={}", msg.getId(), e);
                    }
                }

                // 更新这个会话的已同步数量
                int currentSynced = batchResult.getConversationSyncedCounts()
                        .getOrDefault(conversationId, 0);
                batchResult.getConversationSyncedCounts()
                        .put(conversationId, currentSynced + conversationMessages.size());

                // 更新服务端游标（使用最新消息的序列号）
                Long newCursor = conversationMessages.stream()
                        .map(Message::getServerSeq)
                        .max(Long::compareTo)
                        .orElse(request.getClientCursorMapMap().get(conversationId));
                serverCursorMap.put(conversationId, newCursor);
            }
        }

        boolean hasMore = batchResult.getTotalSynced() + messagesInBatch < batchResult.getTotalCount();

        batchResult.setMessages(messages);
        batchResult.setHasMore(hasMore);
        batchResult.setServerCursorMap(serverCursorMap);
        batchResult.setMaxSeq(maxSeq);
    }

    /**
     * 将MessageContent转换为ImMessage字节数组
     */
    private byte[] convertToImMessageBytes(Message message) {
        ImMessage imMessage = MessageConverter.convertToImMessage(message);
        return imMessage.toByteArray();
    }

    /**
     * 计算全量同步总消息数
     */
    private int calculateTotalFullSyncCount(OfflineSyncRequest request) {
        Map<Long, Integer> syncCounts = calculateFullSyncCounts(request);
        int total = syncCounts.values().stream().mapToInt(Integer::intValue).sum();

        // 应用全局限制
        return Math.min(total, FULL_SYNC_MAX_MESSAGES);
    }

    /**
     * 发送全量同步批次
     */
    private ResponseResult<Void> sendFullSyncBatch(OfflineSyncRequest request, SyncBatchResult batchResult) {
        OfflineSyncResponse offlineSyncResponse = buildFullSyncResponse(request, batchResult);
        MQMessageWrapper wrapper = commonSyncService.buildBatchResponseWrapper(offlineSyncResponse);
        return commonSyncService.sendBatchResponse(wrapper, offlineSyncResponse);
    }

    /**
     * 构建全量同步响应
     */
    private OfflineSyncResponse buildFullSyncResponse(OfflineSyncRequest request, SyncBatchResult batchResult) {
        OfflineSyncResponse.Builder builder = commonSyncService.buildBatchResponse(request, batchResult).toBuilder();

        // 设置全量同步特有字段
        builder.setIsFullSync(true);
        builder.putAllConversationSyncCounts(batchResult.getConversationSyncCounts());
        builder.setMaxSyncMessages(FULL_SYNC_MAX_MESSAGES);

        // 添加提示信息
        if (hasLargeConversations(batchResult.getConversationSyncCounts())) {
            builder.setSyncHint("Some conversations have limited messages, consider manual history pull");
        }

        return builder.build();
    }

    /**
     * 完成全量同步
     */
    private void completeFullSync(SyncSession syncSession, SyncBatchResult batchResult,
                                  Map<Long, Integer> syncCounts) {
        // 发送完成响应
        commonSyncService.sendSyncSession(syncSession, batchResult);

        // 更新会话状态
        syncSessionService.completeSyncSession(batchResult.getSyncId(), batchResult.getTotalSynced());

        log.info("Full sync completed: syncId={}, totalBatches={}, totalMessages={}",
                syncSession.getSyncId(), batchResult.getCurrentBatch(), batchResult.getTotalSynced());
    }

    /**
     * 判断是否有大会话需要历史拉取
     */
    private boolean hasLargeConversations(Map<Long, Integer> syncCounts) {
        return syncCounts.values().stream().anyMatch(count -> count >= 100);
    }

    /**
     * 获取全量同步批次大小
     */
    private int getFullSyncBatchSize(OfflineSyncRequest request) {
        int requestedSize = request.getBatchSize() > 0 ? request.getBatchSize() : DEFAULT_BATCH_SIZE;
        return Math.min(Math.min(requestedSize, MAX_BATCH_SIZE), FULL_SYNC_BATCH_SIZE);
    }

    /**
     * 全量同步消息查询 - 修正为查询主消息表
     */
    private List<Message> getMessagesForFullSync(Long userId, Long conversationId,
                                                 Long maxSeq, int limit) {
        // 查询主消息表 im_message
        return messageService.getRecentMessages(null, conversationId, maxSeq, limit);
    }
}
