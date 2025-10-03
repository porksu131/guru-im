package com.guru.im.disaptch.rocketmq.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import jakarta.annotation.PostConstruct;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DispatchRetryService {
    private static final Logger log = LoggerFactory.getLogger(DispatchRetryService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final GatewayConnector gatewayConnector;
    private final ScheduledExecutorService retryExecutor;
    private final MQMessageSender mqMessageSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 重试配置
    @Value("${guru.im.dispatch.push.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${guru.im.dispatch.push.retry.initial-delay:1000}")
    private long initialDelayMs;

    @Value("${guru.im.dispatch.push.retry.max-delay:10000}")
    private long maxDelayMs;

    @Value("${guru.im.dispatch.push.retry.expiry-time:300000}") // 5分钟
    private long expiryTimeMs;


    public DispatchRetryService(RedisTemplate<String, Object> redisTemplate,
                                GatewayConnector gatewayConnector,
                                ScheduledExecutorService retryExecutor,
                                MQMessageSender mqMessageSender) {
        this.redisTemplate = redisTemplate;
        this.gatewayConnector = gatewayConnector;
        this.retryExecutor = retryExecutor;
        this.mqMessageSender = mqMessageSender;
    }

    /**
     * 初始化重试执行器
     */
    @PostConstruct
    public void init() {
        retryExecutor.scheduleWithFixedDelay(this::processRetries,
                5, 5, TimeUnit.SECONDS); // 每5秒检查一次重试
    }

    /**
     * 处理推送结果并安排重试
     */
    public void handlePushResults(MQMessageWrapper envelope,
                                  Long uid, List<ImMessage> results,
                                  List<DeviceStatus> targetGatewayNodes,
                                  RetryResultHandler retryResultHandler) {
        if (results == null || results.size() != targetGatewayNodes.size()) {
            log.warn("Invalid push results for message: {}, uid: {}", envelope.getMessageId(), uid);
            return;
        }

        // 找出失败的网关节点
        List<DeviceStatus> failedNodes = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            if (ResponseCode.SUCCESS != results.get(i).getResponse().getCode()) {
                failedNodes.add(targetGatewayNodes.get(i));
            }
        }

        if (failedNodes.isEmpty()) {
            return; // 全部成功，无需重试
        }

        // 只有需要可靠投递的消息才重试
        if (envelope.getQos() == MQQos.QOS_AT_LEAST_ONCE) {
            scheduleRetry(envelope, uid, failedNodes, targetGatewayNodes, retryResultHandler);
        } else {
            log.debug("QoS is AT_MOST_ONCE, skipping retry for message: {}", envelope.getMessageId());
        }
    }

    /**
     * 安排重试
     */
    private void scheduleRetry(MQMessageWrapper envelope, Long uid,
                               List<DeviceStatus> failedNodes,
                               List<DeviceStatus> originalNodes,
                               RetryResultHandler retryResultHandler) {
        DispatchRetryContext retryContext = new DispatchRetryContext();
        retryContext.setMessageId(envelope.getMessageId());
        retryContext.setUid(uid);
        retryContext.setMessageContent(envelope);
        retryContext.setTargetGatewayNodes(originalNodes);
        retryContext.setFailedGatewayNodes(failedNodes);
        retryContext.setRetryCount(1);
        retryContext.setNextRetryTime(System.currentTimeMillis() + calculateDelay(1));
        retryContext.setExpiryTime(System.currentTimeMillis() + expiryTimeMs);
        retryContext.setMessageType(envelope.getMessageType());
        retryContext.setQos(envelope.getQos());
        retryContext.setRetryResultHandler(retryResultHandler);

        // 存储重试上下文
        storeRetryContext(retryContext);

        log.info("Scheduled retry for message: {}, uid: {}, failed nodes: {}, attempt: {}",
                envelope.getMessageId(), uid, failedNodes.size(), 1);
    }

    /**
     * 处理重试
     */
    private void processRetries() {
        try {
            // 获取所有需要重试的上下文
            Set<String> keys = redisTemplate.keys("push_retry:*");
            if (keys.isEmpty()) {
                return;
            }
            for (String key : keys) {
                String contextJson = (String) redisTemplate.opsForValue().get(key);
                DispatchRetryContext context = objectMapper.readValue(contextJson, new TypeReference<DispatchRetryContext>() {
                });
                if (context != null && context.shouldRetry()) {
                    executeRetry(context);
                }
            }
        } catch (Exception e) {
            log.error("Error processing push retries", e);
        }
    }

    /**
     * 执行重试
     */
    private void executeRetry(DispatchRetryContext context) {
        try {
            MQMessageWrapper envelope = context.getMessageContent();
            ImMessage imMessage = ImMessage.parseFrom(envelope.getBody());

            // 只重试之前失败的节点
            CompletableFuture<List<ImMessage>> retryResults =
                    gatewayConnector.pushMessageToGateways(context.getUid(), context.getFailedGatewayNodes(), imMessage);

            retryResults.thenAccept(results -> {
                handleRetryResults(context, results);
            }).exceptionally(e -> {
                log.error("Retry failed for message: {}, uid: {}",
                        context.getMessageId(), context.getUid(), e);
                updateRetryContext(context, false);
                return null;
            });

        } catch (Exception e) {
            log.error("Failed to parse message for retry: {}", context.getMessageId(), e);
            updateRetryContext(context, false);
        }
    }

    /**
     * 处理重试结果
     */
    private void handleRetryResults(DispatchRetryContext context, List<ImMessage> results) {
        List<DeviceStatus> stillFailedNodes = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            if (ResponseCode.SUCCESS != results.get(i).getResponse().getCode()) {
                stillFailedNodes.add(context.getFailedGatewayNodes().get(i));
            }
        }

        if (stillFailedNodes.isEmpty()) {
            // 重试成功，清理上下文
            redisTemplate.delete(context.getRedisKey());
            log.info("Retry successful for message: {}, uid: {}",
                    context.getMessageId(), context.getUid());
            if (context.getRetryResultHandler() != null) {
                context.getRetryResultHandler().handleRetrySuccess(context);
            }
        } else {
            // 更新重试上下文
            context.setFailedGatewayNodes(stillFailedNodes);
            context.setRetryCount(context.getRetryCount() + 1);

            if (context.getRetryCount() >= maxRetryAttempts || context.isExpired()) {
                // 重试次数用尽或过期
                handleRetryFailure(context);
            } else {
                // 安排下一次重试
                context.setNextRetryTime(System.currentTimeMillis() +
                        calculateDelay(context.getRetryCount()));
                storeRetryContext(context);

                log.info("Scheduling next retry for message: {}, uid: {}, attempt: {}",
                        context.getMessageId(), context.getUid(), context.getRetryCount());
            }
        }
    }

    /**
     * 处理重试失败
     */
    private void handleRetryFailure(DispatchRetryContext context) {
        log.warn("Push retry failed after {} attempts for message: {}, uid: {}, failed nodes: {}",
                context.getRetryCount(), context.getMessageId(), context.getUid(),
                context.getFailedGatewayNodes());

        // 清理上下文
        redisTemplate.delete(context.getRedisKey());

        // 自定义重试失败后的逻辑处理
        if (context.getRetryResultHandler() != null) {
            context.getRetryResultHandler().handleRetryFailed(context);
        } else {
            // 对于重要消息，可以触发告警
            if (context.getMessageType() == MQMessageType.CONTROL ||
                    context.getMessageType() == MQMessageType.REQUEST) {
                triggerAlert(context);
            }

            // 如果用户完全离线，可以转存为离线消息
            if (isUserCompletelyOffline(context)) {
                storeAsOfflineMessage(context);
            }
        }
    }

    /**
     * 计算重试延迟
     */
    private long calculateDelay(int attempt) {
        long delay = initialDelayMs * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 存储重试上下文
     */
    private void storeRetryContext(DispatchRetryContext context) {
        long ttl = context.getExpiryTime() - System.currentTimeMillis();
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(
                    context.getRedisKey(),
                    contextJson,
                    ttl, TimeUnit.MILLISECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新重试上下文
     */
    private void updateRetryContext(DispatchRetryContext context, boolean success) {
        if (success) {
            redisTemplate.delete(context.getRedisKey());
        } else {
            context.setRetryCount(context.getRetryCount() + 1);
            if (context.getRetryCount() >= maxRetryAttempts) {
                handleRetryFailure(context);
            } else {
                context.setNextRetryTime(System.currentTimeMillis() +
                        calculateDelay(context.getRetryCount()));
                storeRetryContext(context);
            }
        }
    }

    /**
     * 检查用户是否完全离线
     */
    private boolean isUserCompletelyOffline(DispatchRetryContext context) {
        try {
            List<DeviceStatus> currentNodes = gatewayConnector.getUserAllGatewayNodes(context.getUid());
            return currentNodes.isEmpty();
        } catch (Exception e) {
            log.error("Failed to check user online status: {}", context.getUid(), e);
            return false;
        }
    }

    /**
     * 存储为离线消息
     */
    private void storeAsOfflineMessage(DispatchRetryContext context) {
        try {
            log.info("Message stored as offline: {}, uid: {}",
                    context.getMessageId(), context.getUid());
            MQMessageWrapper envelope = context.getMessageContent();
            // 只有需要可靠投递的消息才进行离线存储
            if (envelope.getQos() == MQQos.QOS_AT_LEAST_ONCE) {
                MQMessageWrapper newWrapper = new MQMessageWrapper(envelope);
                newWrapper.setTargetTopic(MQTopic.OFFLINE_TOPIC);
                newWrapper.setTargetTag("store");
                newWrapper.setReceiverIds(Collections.singletonList(context.getUid()));

                sendToMicroservice(newWrapper, newWrapper.getTargetTopic(), newWrapper.getTargetTag());
            }

        } catch (Exception e) {
            log.error("Failed to store offline message: {}", context.getMessageId(), e);
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(DispatchRetryContext context) {
        // 集成告警系统
        String alertMsg = String.format(
                "重要消息推送失败: messageId=%s, uid=%s, type=%s, attempts=%d",
                context.getMessageId(), context.getUid(),
                context.getMessageType(), context.getRetryCount());

        // alertService.sendAlert(alertMsg);
        log.warn(alertMsg);
    }

    protected void sendToMicroservice(MQMessageWrapper wrapper, String topic, String tag) {
        try {
            mqMessageSender.sendAsync(
                    String.format("%s:%s", topic, tag),
                    wrapper,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.debug("Message routed successfully: {}", wrapper.getMessageId());
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("Failed to route message: {}", wrapper.getMessageId(), e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Failed to send message to microservice", e);
        }
    }
}