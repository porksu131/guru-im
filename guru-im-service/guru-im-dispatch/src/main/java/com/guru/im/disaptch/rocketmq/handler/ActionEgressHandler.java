package com.guru.im.disaptch.rocketmq.handler;

import com.guru.im.common.constant.MQTopic;
import com.guru.im.disaptch.rocketmq.handler.base.AbstractMessageHandler;
import com.guru.im.disaptch.service.GatewayConnector;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import org.springframework.stereotype.Component;

@Component
public class ActionEgressHandler extends AbstractMessageHandler {

    public ActionEgressHandler(MQMessageSender mqMessageSender, GatewayConnector gatewayConnector) {
        super(mqMessageSender, gatewayConnector, null);
    }

    @Override
    public boolean canHandle(MQMessageWrapper envelope) {
        return envelope.getMessageType() == MQMessageType.ACTION
                && envelope.getTargetTopic().equals(MQTopic.DISPATCH_ACTION_TOPIC);
    }

    @Override
    public void handle(MQMessageWrapper envelope) {
        pushMessageToUser(envelope);
    }
}