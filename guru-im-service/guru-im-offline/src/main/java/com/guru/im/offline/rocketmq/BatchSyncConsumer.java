package com.guru.im.offline.rocketmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.service.OfflineEventServiceImpl;
import com.guru.im.offline.service.sync.BatchSyncService;
import com.guru.im.offline.service.sync.HistorySyncService;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.OfflineSyncType;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.OFFLINE_TOPIC,
        selectorExpression = MQTag.BATCH_SYNC,
        consumerGroup = "${rocketmq.consumers.sync-group}",
        messageModel = MessageModel.CLUSTERING)
public class BatchSyncConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(BatchSyncConsumer.class);
    @Autowired
    private BatchSyncService batchSyncService;
    @Autowired
    private HistorySyncService historySyncService;
    @Autowired
    private OfflineEventServiceImpl offlineEventService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        try {
            ImMessage imMessage = ImMessage.parseFrom(wrapper.getBody());
            if (imMessage.getBodyCase() == ImMessage.BodyCase.SYNC_REQUEST) {
                if (imMessage.getSyncRequest().getSyncType() == OfflineSyncType.SYNC_TYPE_FULL) {
                    historySyncService.handleFullSyncRequest(imMessage.getSyncRequest());
                } else if (imMessage.getSyncRequest().getSyncType() == OfflineSyncType.SYNC_TYPE_INCREMENTAL) {
                    batchSyncService.handleSyncRequest(imMessage.getSyncRequest());
                }
                return;
            } else if (imMessage.getBodyCase() == ImMessage.BodyCase.SYNC_EVENT_REQUEST) {
                offlineEventService.processOfflineEventsSyncRequest(wrapper, imMessage.getSyncEventRequest());
                return;
            }
            log.warn("can not process message, ignore it {}", imMessage.getBodyCase());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
