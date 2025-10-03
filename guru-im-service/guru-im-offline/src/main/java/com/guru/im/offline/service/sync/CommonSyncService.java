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
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.model.pojo.SyncSession;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class CommonSyncService {


    private static final Logger log = LoggerFactory.getLogger(CommonSyncService.class);
    private final MQMessageSender mqMessageSender;
    private final SyncAckService syncAckService;

    public CommonSyncService(MQMessageSender mqMessageSender,
                             SyncAckService syncAckService) {
        this.mqMessageSender = mqMessageSender;
        this.syncAckService = syncAckService;
    }

    public void sendSyncSession(SyncSession syncSession, SyncBatchResult syncBatchResult) {
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

    public void sendSyncErrorResponse(OfflineSyncRequest request, String errorMsg) {
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

    public void waitForAck(Long syncId, int batchNumber) throws InterruptedException {
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


    public OfflineSyncResponse buildErrorResponse(OfflineSyncRequest request, String errorMsg) {
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

    public MQMessageWrapper buildBatchResponseWrapper(OfflineSyncResponse response) {
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

    public OfflineSyncResponse buildBatchResponse(OfflineSyncRequest request, SyncBatchResult batchResult) {
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
}
