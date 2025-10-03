package com.guru.im.offline.service.sync;

import com.google.protobuf.ByteString;
import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.exception.RetryException;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.model.pojo.OfflineMessageContent;
import com.guru.im.offline.model.pojo.SyncSession;
import com.guru.im.offline.service.OfflineMessageService;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.lang.System;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Component
public class BatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(BatchSyncService.class);
    private final OfflineMessageService offlineMessageService;
    private final MQMessageSender mqMessageSender;
    private final SyncSessionService syncSessionService;
    private final SnowflakeIdGenerator idGenerator;
    private final SyncAckService syncAckService;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 500;

    public BatchSyncService(OfflineMessageService offlineMessageService, MQMessageSender mqMessageSender,
                            SyncSessionService syncSessionService, SnowflakeIdGenerator idGenerator,
                            SyncAckService syncAckService) {
        this.offlineMessageService = offlineMessageService;
        this.mqMessageSender = mqMessageSender;
        this.syncSessionService = syncSessionService;
        this.idGenerator = idGenerator;
        this.syncAckService = syncAckService;
    }

    public void handleSyncRequest(OfflineSyncRequest request) {
        try {
            if (syncSessionService.isSyncInProgress(request.getUserId(), request.getDeviceId())) {
                sendSyncErrorResponse(request, "Sync already in progress");
                return;
            }

            SyncSession syncSession = getAndCreateSyncSession(request);

            processSyncInBatches(request, syncSession);

        } catch (Exception e) {
            log.error("Handle sync request failed", e);
            sendSyncErrorResponse(request, e.getMessage());
        }
    }

    private void processSyncInBatches(OfflineSyncRequest request, SyncSession syncSession) {
        Long syncId = syncSession.getSyncId();
        int batchSize = getBatchSize(request);
        int totalCount = syncSession.getTotalCount();
        try {
            // 初始化批次状态
            SyncBatchResult batch = new SyncBatchResult();
            batch.setSyncId(syncId);
            batch.setBatchSize(getBatchSize(request));
            batch.setHasMore(totalCount > 0);
            batch.setTotalCount(totalCount);
            batch.setTotalBatches((int) Math.ceil((double) totalCount / batchSize));
            batch.setCurrentCursorMap(request.getClientCursorMapMap());
            batch.setConvHasMoreMap(new HashMap<>());
            batch.setCurrentBatch(0);

            while (batch.isHasMore()) {
                // 获取下一批消息
                getNextBatch(request, batch);

                if (batch.getMessages().isEmpty()) {
                    log.info("No more messages to sync: syncId={}", syncId);
                    break;
                }

                // 发送当前批次
                ResponseResult<Void> sendResponse = sendSyncBatch(request, batch);
                if (ResponseResult.isError(sendResponse)) {
                    // 发送失败，清理状态并处理错误
                    syncAckService.cleanupBatchAckState(request.getSyncId(), batch.getCurrentBatch());
                    throw new RuntimeException(sendResponse.getMsg());
                }
                // 发送成功后设置等待ACK状态
                syncAckService.setBatchWaitingForAck(request.getSyncId(), batch.getCurrentBatch());

                batch.setTotalSynced(batch.getTotalSynced() + batch.getMessages().size());

                // 更新同步进度
                syncSessionService.updateSyncProgress(syncId, batch.getTotalSynced(),
                        batch.getCurrentBatch(), batch.getMaxSeq());

                log.debug("Sent batch {}: syncId={}, messages={}, totalSynced={}",
                        batch.getTotalSynced(), syncId, batch.getMessages().size(), batch.getTotalSynced());

                // 如果不是最终批次，等待ACK
                if (batch.isHasMore()) {
                    waitForAck(syncId, batch.getCurrentBatch());
                    batch.setCurrentBatch(batch.getCurrentBatch() + 1);
                } else {
                    break;
                }

                // 安全限制，防止无限循环
                if (batch.getCurrentBatch() > 1000) {
                    log.warn("Batch limit reached: syncId={}", syncId);
                    break;
                }
            }

            // 发送完成同步
            sendSyncSession(syncSession, batch);
            // 更新会话状态为已完成
            syncSessionService.completeSyncSession(batch.getSyncId(), batch.getTotalSynced());

            log.info("Sync completed successfully: syncId={}, totalBatches={}, totalMessages={}",
                    syncId, batch.getCurrentBatch(), batch.getTotalSynced());

        } catch (Exception e) {
            log.error("Process sync batches failed: syncId={}", syncId, e);
            sendSyncErrorResponse(request, e.getMessage());
            syncSessionService.updateSyncStatus(syncId, 5, "Process sync batches failed: " + e.getMessage());
        }
    }

    private void sendSyncSession(SyncSession syncSession, SyncBatchResult syncBatchResult) {
        // 发送完成响应
        OfflineSyncResponse response = OfflineSyncResponse.newBuilder()
                .setSyncId(syncSession.getSyncId())
                .setSyncType(OfflineSyncType.forNumber(syncSession.getSyncType()))
                .setUserId(syncSession.getUserId())
                .setDeviceId(syncSession.getDeviceId())
                .setTotalBatches(syncBatchResult.getTotalBatches())
                .setTotalCount(syncBatchResult.getTotalCount())
                .setHasMore(syncBatchResult.isHasMore())
                .setCurrentBatch(syncBatchResult.getCurrentBatch())
                .setSyncStatus(SyncStatus.SYNC_STATUS_COMPLETE) // 已完成
                .build();

        MQMessageWrapper wrapper = buildBatchResponseWrapper(response);
        mqMessageSender.sendAsync(wrapper.getDestination(), wrapper, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.debug("send sync complete response successfully");
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("send sync complete response failed", throwable);
            }
        });
    }

    private void sendSyncErrorResponse(OfflineSyncRequest request, String errorMsg) {
        OfflineSyncResponse offlineSyncResponse = buildErrorResponse(request, errorMsg);
        MQMessageWrapper wrapper = buildBatchResponseWrapper(offlineSyncResponse);
        mqMessageSender.sendAsync(wrapper.getDestination(), wrapper, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.debug("Sync request send error response successfully");
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("Sync request send error response failed, {}", throwable.getMessage(), throwable);
            }
        });
    }

    private SyncSession getAndCreateSyncSession(OfflineSyncRequest request) {
        long clientSyncId = request.getSyncId();
        if (clientSyncId > 0) {
            SyncSession existingSession = syncSessionService.getSyncSession(clientSyncId);
            if (isSessionActive(existingSession)) {
                throw new RuntimeException("Sync session already exists");
            }
            return existingSession;
        }
        return createSyncSession(request);
    }

    private SyncSession createSyncSession(OfflineSyncRequest request) {
        SyncSession session = new SyncSession();
        session.setId(idGenerator.nextId());
        session.setSyncId(idGenerator.nextId());
        session.setUserId(request.getUserId());
        session.setDeviceId(request.getDeviceId());
        session.setSyncType(request.getSyncTypeValue());
        session.setBatchSize(getBatchSize(request));
        session.setSyncStatus(SyncStatus.SYNC_STATUS_IN_PROGRESS_VALUE);
        session.setStartTime(System.currentTimeMillis());
        session.setCreateTime(System.currentTimeMillis());
        session.setUpdateTime(System.currentTimeMillis());
        session.setLastActivityTime(System.currentTimeMillis());
        session.setTotalCount(getTotalMessages(request)); // 计算消息总数
        return syncSessionService.createSyncSession(session);
    }

    private int getTotalMessages(OfflineSyncRequest request) {
        int totalMessages = 0;
        for (Map.Entry<Long, Long> entry : request.getClientCursorMapMap().entrySet()) {
            Integer count = offlineMessageService.countByUserAndConversation(
                    request.getUserId(), null, entry.getKey(), entry.getValue());
            totalMessages += count != null ? count : 0;
        }
        return totalMessages;
    }

    private ResponseResult<Void> sendSyncBatch(OfflineSyncRequest request, SyncBatchResult batchResult) {
        OfflineSyncResponse offlineSyncResponse = buildBatchResponse(request, batchResult);
        MQMessageWrapper wrapper = buildBatchResponseWrapper(offlineSyncResponse);

        return sendBatchResponse(wrapper, offlineSyncResponse);
    }

    private void getNextBatch(OfflineSyncRequest request, SyncBatchResult batchResult) {
        Map<Long, Long> serverCursorMap = new HashMap<>();
        List<byte[]> messages = new ArrayList<>();
        long maxSeq = 0L;
        boolean hasMore = false;
        int messagesInBatch = 0;

        // 遍历所有会话，为每个会话获取一批消息
        for (Map.Entry<Long, Long> entry : batchResult.getCurrentCursorMap().entrySet()) {
            Long conversationId = entry.getKey();
            Long currentCursor = entry.getValue();

            // 当前会话如果没有离线消息，跳过，查询下一个会话
            if (batchResult.getConvHasMoreMap().get(conversationId) != null
                    && !batchResult.getConvHasMoreMap().get(conversationId)) {
                continue;
            }

            if (messagesInBatch >= batchResult.getBatchSize()) {
                // 当前批次已满，标记还有更多消息
                hasMore = true;
                break;
            }

            // 计算这个会话在当前批次中还能获取多少条消息
            int remainingInBatch = batchResult.getBatchSize() - messagesInBatch;
            if (remainingInBatch <= 0) break;

            // 按最新的游标位点，获取这个会话的一批消息
            List<OfflineMessageContent> conversationMessages = getMessagesForConversation(
                    request.getUserId(), conversationId, currentCursor, remainingInBatch);

            if (!conversationMessages.isEmpty()) {
                // 添加到总消息列表
                for (OfflineMessageContent msg : conversationMessages) {
                    messages.add(msg.getMessageContent());
                    messagesInBatch++;

                    // 更新最大序列号
                    maxSeq = Math.max(maxSeq, msg.getMessageSeq());

                    if (messagesInBatch >= batchResult.getBatchSize()) break;
                }

                // 更新这个会话的游标
                Long newCursor = conversationMessages.stream()
                        .map(OfflineMessageContent::getMessageSeq)
                        .max(Long::compareTo)
                        .orElse(currentCursor);

                serverCursorMap.put(conversationId, newCursor);

                // 检查这个会话是否还有更多消息
                boolean conversationHasMore = checkConversationHasMore(
                        request.getUserId(), conversationId, newCursor);

                if (conversationHasMore) {
                    hasMore = true;
                }
            } else {
                batchResult.getConvHasMoreMap().put(conversationId, false);
            }
        }

        // 检查是否所有会话都没有消息了
        if (!hasMore) {
            hasMore = checkGlobalHasMore(request, serverCursorMap);
        }

        batchResult.setMessages(messages);
        batchResult.setTotalSynced(batchResult.getTotalSynced() + messagesInBatch);
        batchResult.setHasMore(hasMore);
        batchResult.setServerCursorMap(serverCursorMap);
        batchResult.setMaxSeq(maxSeq);
    }

    private List<OfflineMessageContent> getMessagesForConversation(Long userId, Long conversationId,
                                                                   Long currentCursor, int limit) {
        Long currentTime = System.currentTimeMillis();
        return offlineMessageService.getOfflineMessages(
                userId, null, conversationId, currentCursor, limit);
    }

    private boolean checkConversationHasMore(Long userId, Long conversationId, Long currentCursor) {
        Integer remaining = offlineMessageService.countByUserAndConversation(
                userId, null, conversationId, currentCursor);
        return remaining != null && remaining > 0;
    }

    private boolean checkGlobalHasMore(OfflineSyncRequest request, Map<Long, Long> currentCursors) {
        // 检查是否还有任何会话有更多消息
        for (Map.Entry<Long, Long> entry : currentCursors.entrySet()) {
            boolean hasMore = checkConversationHasMore(request.getUserId(), entry.getKey(), entry.getValue());
            if (hasMore) return true;
        }
        return false;
    }

    private MQMessageWrapper buildBatchResponseWrapper(OfflineSyncResponse response) {
        ImMessage imMessage = MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(MessageType.OFFLINE_MSG_RESPONSE)
                .setSyncResponse(response)
                .build();
        MQMessageWrapper wrapper = new MQMessageWrapper();
        wrapper.setSourceType(SourceType.OFFLINE_SERVICE);
        wrapper.setMessageType(MQMessageType.BATCH_SYNC);
        wrapper.setTargetTopic(MQTopic.DISPATCH_CHAT_TOPIC);
        wrapper.setReplyTopic(MQTopic.OFFLINE_TOPIC);
        wrapper.setReplyTag(MQTag.ACK);
        wrapper.setDeviceId(response.getDeviceId());
        wrapper.setCorrelationId(String.valueOf(response.getSyncId()));
        wrapper.setCorrelationType(CorrelationType.BATCH_OFFLINE_SYNC);
        wrapper.setQos(MQQos.QOS_AT_LEAST_ONCE);
        wrapper.setReceiverIds(Collections.singletonList(response.getUserId()));
        wrapper.setBody(imMessage.toByteArray());
        return wrapper;
    }

    private OfflineSyncResponse buildBatchResponse(OfflineSyncRequest request, SyncBatchResult batchResult) {
        OfflineSyncResponse.Builder builder = OfflineSyncResponse.newBuilder();

        builder.setSyncId(batchResult.getSyncId());
        builder.setSyncType(request.getSyncType());
        builder.setUserId(request.getUserId());
        builder.setDeviceId(request.getDeviceId());
        builder.addAllMessages(batchResult.getMessages().stream()
                .map(ByteString::copyFrom)
                .collect(Collectors.toList()));
        builder.putAllServerCursorMap(batchResult.getServerCursorMap());
        builder.setHasMore(batchResult.isHasMore());
        builder.setCurrentBatch(batchResult.getCurrentBatch());
        builder.setTotalCount(batchResult.getTotalCount());
        builder.setTotalBatches(batchResult.getTotalBatches());
        builder.setSyncStatus(SyncStatus.SYNC_STATUS_IN_PROGRESS);
        builder.setFinal(!batchResult.isHasMore());

        return builder.build();
    }

    private OfflineSyncResponse buildErrorResponse(OfflineSyncRequest request, String errorMsg) {
        OfflineSyncResponse.Builder builder = OfflineSyncResponse.newBuilder();

        builder.setSyncId(request.getSyncId());
        builder.setSyncType(request.getSyncType());
        builder.setUserId(request.getUserId());
        builder.setDeviceId(request.getDeviceId());
        builder.setSyncStatus(SyncStatus.SYNC_STATUS_FAILED);
        builder.setErrorMsg(errorMsg);
        builder.setErrorCode(determineErrorCode(errorMsg));

        return builder.build();
    }

    /**
     * 根据错误信息确定错误码
     */
    private int determineErrorCode(String errorMsg) {
        if (errorMsg.contains("already in progress")) {
            return 1001; // 同步正在进行中
        } else if (errorMsg.contains("session already exists")) {
            return 1002; // 会话已存在
        } else if (errorMsg.contains("timeout")) {
            return 1003; // 超时
        } else {
            return 500; // 服务器内部错误
        }
    }

    @Retryable(retryFor = RetryException.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ResponseResult<Void> sendBatchResponse(MQMessageWrapper wrapper, OfflineSyncResponse response) {
        try {
            CompletableFuture<ResponseResult<Void>> future = new CompletableFuture<>();
            mqMessageSender.sendAsync(wrapper.getDestination(), wrapper, new SendCallback() {

                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("Batch {}/{} sent: syncId={}, messages={}",
                            response.getCurrentBatch(), response.getTotalBatches(),
                            response.getSyncId(), response.getTotalCount());
                    future.complete(ResponseResult.ok());
                }

                @Override
                public void onException(Throwable throwable) {
                    future.complete(ResponseResult.fail(throwable.getMessage()));
                }
            });

            ResponseResult<Void> sendResponse = future.get();
            if (ResponseResult.isError(sendResponse)) {
                if (shouldRetryBatchSend(sendResponse.getMsg())) {
                    log.error("Send batch {} failed: syncId={}", response.getCurrentBatch(), response.getSyncId());
                    throw new RetryException("Send failed with: " + sendResponse.getMsg());
                }
            }
            return sendResponse;

        } catch (Exception e) {
            log.error("Failed to send batch {}: syncId={}", response.getCurrentBatch(), response.getSyncId(), e);
            // 记录失败指标
            // meterRegistry.counter("mq.send.failure").increment();
            throw new RetryException("Send operation failed", e);
        }
    }

    // 自动绑定 - 参数和返回类型匹配
    @Recover
    public ResponseResult<Void> recoverSendBatchResponse(RetryException e, MQMessageWrapper wrapper, OfflineSyncResponse response) {
        log.warn("重试3次后仍然失败，返回降级结果");
        return ResponseResult.fail(e.getMessage());
    }

    /**
     * 判断是否应该重试批次发送
     */
    private boolean shouldRetryBatchSend(String error) {
        if (error == null) return false;

        // 可重试的错误类型
        return error.contains("timeout") ||
                error.contains("network") ||
                error.contains("connection") ||
                error.contains("retry") ||
                error.contains("temporary");
    }

    private int calculateEstimatedBatches(OfflineSyncRequest request, SyncBatchResult firstBatch) {
        try {
            long totalMessages = 0;
            for (Map.Entry<Long, Long> entry : request.getClientCursorMapMap().entrySet()) {
                Integer count = offlineMessageService.countByUserAndConversation(
                        request.getUserId(), null, entry.getKey(), entry.getValue());
                totalMessages += count != null ? count : 0;
            }

            int batchSize = getBatchSize(request);
            int estimatedBatches = (int) Math.ceil((double) totalMessages / batchSize);
            return Math.max(1, estimatedBatches); // 至少1个批次
        } catch (Exception e) {
            log.warn("Failed to calculate estimated batches: {}", e.getMessage());
            return 1; // 默认返回1个批次
        }
    }

    private void waitForAck(Long syncId, int batchNumber) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30秒超时

        // 设置等待状态
        syncAckService.setBatchWaitingForAck(syncId, batchNumber);

        try {
            while (System.currentTimeMillis() - startTime < timeout) {
                if (syncAckService.isBatchAcknowledged(syncId, batchNumber)) {
                    log.debug("Batch {} ACK received: syncId={}", batchNumber, syncId);
                    return;
                }

                // 检查是否超时
                if (syncAckService.isBatchTimeout(syncId, batchNumber)) {
                    throw new RuntimeException("Wait for ACK timeout for batch: " + batchNumber);
                }

                Thread.sleep(100); // 短暂休眠
            }

            throw new RuntimeException("Wait for ACK timeout for batch: " + batchNumber);

        } finally {
            // 无论成功还是超时，都清理等待状态
            syncAckService.cleanupBatchAckState(syncId, batchNumber);
        }
    }

    private boolean isSessionActive(SyncSession session) {
        return session != null &&
                (session.getSyncStatus() == 1 || session.getSyncStatus() == 2); // 进行中或已推送
    }

    private int getBatchSize(OfflineSyncRequest request) {
        return request.getBatchSize() > 0 ?
                Math.min(request.getBatchSize(), MAX_BATCH_SIZE) : DEFAULT_BATCH_SIZE;
    }

}