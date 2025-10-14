package com.guru.im.disaptch.rocketmq.handler.base;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.disaptch.rocketmq.retry.DispatchRetryService;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.model.Response;
import com.guru.im.protocol.util.MessageBuilder;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractMessageHandler.class);
    protected final MQMessageSender mqMessageSender;
    protected final GatewayConnector gatewayConnector;
    protected final DispatchRetryService dispatchRetryService;


    public AbstractMessageHandler(MQMessageSender mqMessageSender,
                                  GatewayConnector gatewayConnector,
                                  DispatchRetryService dispatchRetryService) {
        this.mqMessageSender = mqMessageSender;
        this.gatewayConnector = gatewayConnector;
        this.dispatchRetryService = dispatchRetryService;
    }


    /**
     * 处理发给特定用户消息（发往用户）
     */
    protected void pushMessageToUser(MQMessageWrapper envelope) {
        List<Long> receiverIds = envelope.getReceiverIds();

        for (Long uid : receiverIds) {
            try {
                // 获取用户连接的网关节点
                List<DeviceStatus> deviceStatus = gatewayConnector.getUserAllGatewayNodes(uid);
                if (deviceStatus == null || deviceStatus.isEmpty()) {
                    log.error("failed to push , no device found for uid:{}", uid);
                    handlePushError(envelope, uid, "no device found for user");
                    continue;
                }

                // 处理离线设备信息
                handleOfflineDeviceMessage(envelope, uid, deviceStatus);

                // 处理在线设备信息
                handleOnlineDeviceMessage(envelope, uid, deviceStatus);

            } catch (Exception e) {
                log.error("Failed to push message to user: {}", uid, e);
                handlePushError(envelope, uid, e.getMessage());
            }
        }
    }

    protected void handlePushError(MQMessageWrapper envelope, Long uid, String errorMsg) {
        if (MQQos.QOS_AT_LEAST_ONCE == envelope.getQos() && envelope.getReplyTopic() != null) {
            ImMessage imResponse = MessageBuilder.createDefaultImMessage().toBuilder()
                    .setMsgType(ImMessage.MsgType.RESPONSE)
                    .setMessageType(MessageType.ACK)
                    .setResponse(Response.newBuilder()
                            .setCode(ResponseCode.SYSTEM_ERROR)
                            .setMsg(errorMsg)
                            .build())
                    .build();
            replyToMicroService(envelope, uid, imResponse);
        }
    }

    protected void handleOnlineDeviceMessage(MQMessageWrapper envelope, Long uid, List<DeviceStatus> deviceStatusList)
            throws InvalidProtocolBufferException {
        List<DeviceStatus> onlineGatewayNodes = deviceStatusList.stream()
                .filter(obj -> obj.getOnlineStatus() == OnlineStatus.ONLINE)
                .toList();
        if (onlineGatewayNodes.isEmpty()) {
            log.info("uid:{}  offline", uid);
            return;
        }
        // 推送到所有在线设备所连接的网关节点
        ImMessage imMessage = ImMessage.parseFrom(envelope.getBody());
        if (MQQos.QOS_AT_LEAST_ONCE == envelope.getQos()) {
            // 推送双向消息
            CompletableFuture<List<ImMessage>> pushResults =
                    gatewayConnector.pushMessageToGateways(uid, deviceStatusList, imMessage);

            // 需处理响应结果
            pushResults.thenAccept(results ->
                    handlePushResults(envelope, uid, results, deviceStatusList)
            );
        } else {
            // 推送单向消息，无响应
            gatewayConnector.pushMessageByOneway(uid, deviceStatusList, imMessage);
        }
    }

    protected void handleOfflineDeviceMessage(MQMessageWrapper envelope, Long uid, List<DeviceStatus> gatewayNodes) {
        List<DeviceStatus> offlineGatewayNodes = gatewayNodes.stream()
                .filter(obj -> obj.getOnlineStatus() == OnlineStatus.OFFLINE)
                .toList();

        if (!offlineGatewayNodes.isEmpty()) {
            // 处理离线设备信息
            handleUserOffline(envelope, uid, offlineGatewayNodes);
        }
    }


    /**
     * 通用消息发送方法（发往微服务）
     */
    protected void sendToMicroservice(MQMessageWrapper wrapper) {
        try {
            mqMessageSender.sendAsync(
                    wrapper.getDestination(),
                    wrapper,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.debug("Message routed successfully: {}", wrapper.getMessageId());
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("Failed to route message: {}", wrapper.getMessageId(), e);
                            handleMQSendFailure(wrapper, e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Failed to send message to microservice", e);
            handleMQSendFailure(wrapper, e);
        }
    }


    /**
     * 处理用户离线的逻辑
     */
    protected void handleUserOffline(MQMessageWrapper envelope, Long uid, List<DeviceStatus> offlineGatewayNodes) {
        List<String> deviceIds = offlineGatewayNodes.stream().map(DeviceStatus::getDeviceId).toList();
        // 只有需要可靠投递的消息才进行离线存储
        if (envelope.getQos() == MQQos.QOS_AT_LEAST_ONCE) {
            MQMessageWrapper newWrapper = new MQMessageWrapper(envelope);
            newWrapper.setSourceType(SourceType.DISPATCH_SERVICE);
            newWrapper.setTargetTopic(MQTopic.OFFLINE_TOPIC);
            newWrapper.setTargetTag(MQTag.OFFLINE);
            newWrapper.setReceiverIds(Collections.singletonList(uid));
            newWrapper.setOfflineDeviceIds(deviceIds);
            sendToMicroservice(newWrapper);
            log.info("user={}, device=[{}], is offline, message stored: {}", uid,
                    String.join(",", deviceIds), envelope.getMessageId());
            return;
        }

        log.info("user={}, device=[{}], is offline, and message is not need to store: {}", uid,
                String.join(",", deviceIds),
                envelope.getMessageId());
    }

    protected void replyToMicroService(MQMessageWrapper envelope, Long uid, ImMessage imResponse) {
        MQMessageWrapper newWrapper = new MQMessageWrapper(envelope);
        newWrapper.setMessageType(MQMessageType.ACK);
        newWrapper.setSourceType(SourceType.DISPATCH_SERVICE);
        newWrapper.setTargetTopic(envelope.getReplyTopic());
        newWrapper.setTargetTag(envelope.getReplyTopic());
        newWrapper.setReceiverIds(Collections.singletonList(uid));
        newWrapper.setBody(imResponse.toByteArray());
        newWrapper.setReplyTopic(null);
        newWrapper.setReplyTag(null);

        sendToMicroservice(newWrapper);
    }


    /**
     * 处理消息推送结果
     */
    protected void handlePushResults(MQMessageWrapper envelope, Long uid, List<ImMessage> results, List<DeviceStatus> gatewayNodes) {
        boolean allSuccess = results.stream().allMatch(imMessage -> {
            return ResponseCode.SUCCESS == imMessage.getResponse().getCode();
        });
        if (allSuccess) {
            if (envelope.getQos() == MQQos.QOS_AT_LEAST_ONCE && envelope.getReplyTopic() != null) {
                replyToMicroService(envelope, uid, results.get(0));
            }
        } else {
            log.warn("Message partial delivery failed for user: {}, messageId: {}", uid, envelope.getMessageId());
            // 触发重试机制
            dispatchRetryService.handlePushResults(envelope, uid, results, gatewayNodes, null);
        }
    }


    /**
     * 处理MQ发送失败（模板方法，子类可重写）
     */
    protected void handleMQSendFailure(MQMessageWrapper wrapper, Throwable e) {
        // 仅记录日志
        log.error("Message send failed, will be handled by RocketMQ retry mechanism: {}",
                wrapper.getMessageId());

        // todo
    }
}