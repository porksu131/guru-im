package com.guru.im.offline.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.service.sync.SyncAckService;
import com.guru.im.protocol.model.ImMessage;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.OFFLINE_TOPIC,
        selectorExpression = MQTag.BATCH_ACK,
        consumerGroup = "${rocketmq.consumers.syncAck-group}",
        messageModel = MessageModel.CLUSTERING)
public class BatchAckConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(BatchAckConsumer.class);
    @Autowired
    private SyncAckService syncAckService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.SYNC_ACK) {
                syncAckService.processMessage(imMessage.getSyncAck());
                return;
            }
            log.warn("can not process message, ignore it {}", imMessage.getBodyCase());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
