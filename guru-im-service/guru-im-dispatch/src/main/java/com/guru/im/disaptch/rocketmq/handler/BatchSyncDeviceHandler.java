package com.guru.im.disaptch.rocketmq.handler;

import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.constant.ResponseCode;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.common.model.MQQos;
import com.guru.im.disaptch.rocketmq.handler.base.AbstractMessageHandler;
import com.guru.im.disaptch.rocketmq.retry.DispatchRetryService;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.MessageType;
import com.guru.im.protocol.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Component
public class BatchSyncDeviceHandler extends AbstractMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(BatchSyncDeviceHandler.class);

    public BatchSyncDeviceHandler(MQMessageSender mqMessageSender,
                                  GatewayConnector gatewayConnector,
                                  DispatchRetryService dispatchRetryService) {
        super(mqMessageSender, gatewayConnector, dispatchRetryService);
    }

    @Override
    public boolean canHandle(MQMessageWrapper wrapper) {
        return wrapper.getMessageType() == MQMessageType.BATCH_SYNC &&
                wrapper.getSourceType() == SourceType.OFFLINE_SERVICE &&
                wrapper.getReceiverIds() != null && wrapper.getReceiverIds().size() == 1;
    }

    @Override
    public void handle(MQMessageWrapper envelope) {
        long uid = envelope.getReceiverIds().get(0);
        String deviceId = envelope.getDeviceId();
        try {
            ImMessage imRequest = ImMessage.parseFrom(envelope.getBody());
            // 获取用户设备连接的网关节点
            DeviceStatus gatewayNode = gatewayConnector.getUserDeviceGatewayNode(uid, deviceId);
            if (gatewayNode == null) {
                log.error("failed to push , no device found for uid:{}", uid);
                return;
            }

            // 处理离线消息
            if (gatewayNode.getOnlineStatus() == OnlineStatus.OFFLINE) {
                ImMessage imResponse = MessageBuilder.createImResponse(imRequest,
                        ResponseCode.SYSTEM_ERROR,
                        "user device offline");
                handlePushResults(envelope, uid, imResponse);
                return;
            }

            // 在线，推送到用户设备连接的网关节点
            CompletableFuture<ImMessage> pushResult = gatewayConnector.pushMessageToGateway(uid, gatewayNode, imRequest);

            pushResult.thenAccept(result -> handlePushResults(envelope, uid, result));

        } catch (Exception e) {
            log.error("Failed to push message to user: {}", uid, e);
        }
    }

    public void handlePushResults(MQMessageWrapper envelope, long uid, ImMessage result) {
        if (envelope.getQos() == MQQos.QOS_AT_LEAST_ONCE) {
            MQMessageWrapper newWrapper = new MQMessageWrapper(envelope);
            newWrapper.setSourceType(SourceType.DISPATCH_SERVICE);
            newWrapper.setMessageType(result.getMessageType() == MessageType.OFFLINE_MSG_ACK ?
                    MQMessageType.BATCH_ACK : MQMessageType.ACK);
            newWrapper.setTargetTopic(envelope.getReplyTopic());
            newWrapper.setTargetTag(result.getMessageType() == MessageType.OFFLINE_MSG_ACK ?
                    MQTag.BATCH_ACK : envelope.getReplyTag());
            newWrapper.setReceiverIds(Collections.singletonList(uid));
            newWrapper.setBody(result.toByteArray());

            sendToMicroservice(newWrapper);
        }
    }
}
