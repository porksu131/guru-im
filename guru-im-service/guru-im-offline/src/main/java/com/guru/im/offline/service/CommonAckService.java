package com.guru.im.offline.service;

import com.guru.im.common.constant.CorrelationType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonAckService {
    @Autowired
    private OfflineMessageService offlineMessageService;
    @Autowired
    private OfflineEventServiceImpl offlineEventService;

    public void processMessage(MQMessageWrapper messageWrapper, ImMessage imMessage) {
        if (messageWrapper.getCorrelationType() == CorrelationType.BATCH_EVENTS_SYNC
                && imMessage.getBodyCase() == ImMessage.BodyCase.RESPONSE) {
            offlineEventService.processCommonAck(Long.parseLong(messageWrapper.getCorrelationId()), imMessage.getResponse());
        }
    }
}
