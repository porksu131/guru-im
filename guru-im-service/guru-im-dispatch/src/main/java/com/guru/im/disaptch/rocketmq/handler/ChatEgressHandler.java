package com.guru.im.disaptch.rocketmq.handler;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.disaptch.rocketmq.handler.base.AbstractMessageHandler;
import com.guru.im.disaptch.rocketmq.retry.DispatchRetryService;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// 用户消息下行处理器
@Component
public class ChatEgressHandler extends AbstractMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatEgressHandler.class);

    public ChatEgressHandler(MQMessageSender mqMessageSender,
                             GatewayConnector gatewayConnector,
                             DispatchRetryService dispatchRetryService) {
        super(mqMessageSender, gatewayConnector, dispatchRetryService);
    }

    @Override
    public boolean canHandle(MQMessageWrapper envelope) {
        return envelope.getMessageType() == MQMessageType.CHAT
                && envelope.getTargetTopic().equals(MQTopic.DISPATCH_CHAT_TOPIC);
    }

    @Override
    public void handle(MQMessageWrapper envelope) {
        pushMessageToUser(envelope);
    }
}