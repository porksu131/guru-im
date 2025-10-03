package com.guru.im.single.service;

import com.guru.im.common.constant.CorrelationType;
import com.guru.im.common.constant.DeliveryStatus;
import com.guru.im.common.constant.ResponseCode;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AckProcessService {
    private static final Logger log = LoggerFactory.getLogger(AckProcessService.class);
    private final ReadReceiptService readReceiptService;
    private final SingleChatService singleChatService;

    public AckProcessService(ReadReceiptService readReceiptService,
                             SingleChatService singleChatService) {
        this.readReceiptService = readReceiptService;
        this.singleChatService = singleChatService;
    }


    public void processMessage(MQMessageWrapper wrapper, ImMessage imMessage) {
        if (imMessage.getBodyCase() != ImMessage.BodyCase.RESPONSE) {
            log.warn("Ignoring im message [{}] because it is not a response message", imMessage.getMsgId());
            return;
        }
        Response response = imMessage.getResponse();
        if (wrapper.getCorrelationType() == CorrelationType.CHAT_MESSAGE) {
            processChatMessageAck(wrapper, response);
        } else if (wrapper.getCorrelationType() == CorrelationType.READ_RECEIPT_NOTIFY) {
            processReadReceiptAck(wrapper, response);
        }
    }


    private void processChatMessageAck(MQMessageWrapper wrapper, Response response) {
        if (ResponseCode.SUCCESS == response.getCode()) {
            singleChatService.updateDeliveryStatus(Long.parseLong(wrapper.getCorrelationId()), DeliveryStatus.ARRIVED);
        }
    }

    private void processReadReceiptAck(MQMessageWrapper wrapper, Response response) {
        if (ResponseCode.SUCCESS == response.getCode()) {
            readReceiptService.updateDeliveryStatus(Long.parseLong(wrapper.getCorrelationId()), DeliveryStatus.ARRIVED);
        }
    }

}
