package com.guru.im.offline.rocketmq;

import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.common.model.MQMessageType;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.offline.service.OfflineEventServiceImpl;
import com.guru.im.offline.service.OfflineMessageService;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = MQTopic.OFFLINE_TOPIC,
        selectorExpression = MQTag.OFFLINE,
        consumerGroup = "${rocketmq.consumers.offline-group}",
        messageModel = MessageModel.CLUSTERING)
public class OfflineMessageConsumer implements RocketMQListener<MQMessageWrapper> {
    private static final Logger log = LoggerFactory.getLogger(OfflineMessageConsumer.class);
    @Autowired
    private OfflineMessageService offlineMessageService;
    @Autowired
    private OfflineEventServiceImpl offlineEventService;

    @Override
    public void onMessage(MQMessageWrapper wrapper) {
        if (wrapper.getMessageType() == MQMessageType.CHAT) {
            offlineMessageService.saveOfflineChatMessage(wrapper);
        } else {
            offlineEventService.processSaveOfflineEvent(wrapper);
        }
    }
}