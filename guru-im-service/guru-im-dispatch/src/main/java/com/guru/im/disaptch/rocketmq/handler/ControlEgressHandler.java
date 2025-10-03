package com.guru.im.disaptch.rocketmq.handler;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.model.DeviceStatus;
import com.guru.im.disaptch.rocketmq.handler.base.AbstractMessageHandler;
import com.guru.im.disaptch.rocketmq.retry.DispatchRetryService;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// 控制消息处理器（高优先级）

@Component
public class ControlEgressHandler extends AbstractMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ControlEgressHandler.class);

    public ControlEgressHandler(MQMessageSender mqMessageSender,
                                GatewayConnector gatewayConnector,
                                DispatchRetryService dispatchRetryService) {
        super(mqMessageSender, gatewayConnector, dispatchRetryService);
    }

    @Override
    public boolean canHandle(MQMessageWrapper envelope) {
        return envelope.getMessageType() == MQMessageType.CONTROL
                && envelope.getTargetTopic().equals(MQTopic.DISPATCH_CONTROL_TOPIC);
    }

    @Override
    public void handle(MQMessageWrapper envelope) {
        // 控制消息立即处理，最高优先级
        if (!envelope.getReceiverIds().isEmpty()) {
            Long uid = envelope.getReceiverIds().get(0);
            try {
                List<DeviceStatus> gatewayNodes = gatewayConnector.getUserDeviceStatus(uid, OnlineStatus.ONLINE);

                if (!gatewayNodes.isEmpty()) {
                    ImMessage imMessage = ImMessage.parseFrom(envelope.getBody());
                    CompletableFuture<List<ImMessage>> pushResults =
                            gatewayConnector.pushMessageToGateways(uid, gatewayNodes, imMessage);

                    pushResults.thenAccept(results ->
                            handlePushResults(envelope, uid, results, gatewayNodes)
                    );
                }
            } catch (Exception e) {
                log.error("Failed to push message to user: {}", uid, e);
            }
        }
    }
}