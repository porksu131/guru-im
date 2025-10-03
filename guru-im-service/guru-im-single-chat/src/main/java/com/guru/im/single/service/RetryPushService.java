package com.guru.im.single.service;

import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
public class RetryPushService {
    private static final Logger log = LoggerFactory.getLogger(RetryPushService.class);
    private final MQMessageSender mqMessageSender;
    private final Executor messagePushExecutor;

    public RetryPushService(MQMessageSender mqMessageSender, Executor messagePushExecutor) {
        this.mqMessageSender = mqMessageSender;
        this.messagePushExecutor = messagePushExecutor;
    }

    /**
     * 异步推送消息到分发层（支持重试）
     */
    public void AsyncPushToDispatch(MQMessageWrapper wrapper, RetryPushListener retryPushListener) {
        messagePushExecutor.execute(() -> {
            pushToDispatchWithRetry(wrapper, retryPushListener);
        });
    }

    private void pushToDispatchWithRetry(MQMessageWrapper wrapper, RetryPushListener retryPushListener) {
        int maxRetries = 3;
        long initialDelay = 1000; // 1秒
        long maxDelay = 10000;    // 10秒

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                boolean success = pushToDispatch(wrapper); // 推送

                if (success) {
                    log.debug("消息推送成功: correlationId={}, attempt={}", wrapper.getCorrelationId(), attempt);
                    if (retryPushListener != null) {
                        retryPushListener.onPushSuccess(attempt - 1);
                    }
                    return; // 推送成功，退出重试
                } else {
                    log.warn("消息推送失败: correlationId={}, attempt={}", wrapper.getCorrelationId(), attempt);
                }

            } catch (Exception e) {
                log.error("消息推送异常: correlationId={}, attempt={}", wrapper.getCorrelationId(), attempt, e);
            }

            // 计算下一次重试的延迟时间（指数退避）
            if (attempt < maxRetries) {
                long delay = calculateBackoffDelay(attempt, initialDelay, maxDelay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("消息推送重试次数耗尽: correlationId={}", wrapper.getCorrelationId());
        retryPushListener.onPushFailed(maxRetries);
    }

    /**
     * 计算指数退避延迟时间
     */
    private long calculateBackoffDelay(int attempt, long initialDelay, long maxDelay) {
        long delay = initialDelay * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, maxDelay);
    }

    /**
     * 推送消息到分发层
     */
    private boolean pushToDispatch(MQMessageWrapper wrapper) {
        SendResult sendResult = mqMessageSender.sendSync(wrapper.getDestination(), wrapper);
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }
}
