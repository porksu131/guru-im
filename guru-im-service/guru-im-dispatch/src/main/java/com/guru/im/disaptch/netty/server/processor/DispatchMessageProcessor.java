package com.guru.im.disaptch.netty.server.processor;

import com.guru.im.cache.starter.distribute.id.SequenceIdGenerator;
import com.guru.im.common.constant.SourceType;
import com.guru.im.common.model.ImMessageHelper;
import com.guru.im.common.model.MessageBusinessMeta;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.protocol.model.ChatMessageAck;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.protocol.util.MessageUpdater;
import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DispatchMessageProcessor implements MessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchMessageProcessor.class);
    @Autowired
    private MQMessageSender mqMessageSender;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private SequenceIdGenerator sequenceIdGenerator;

    @Override
    public ImMessage processRequest(ChannelHandlerContext ctx, ImMessage message) throws Exception {
        ImMessage request = message;
        try {
            // 提取业务元数据
            MessageBusinessMeta metaData = ImMessageHelper.extractBusinessMQMeta(request);
            Response response = MessageBuilder.createResponse(ResponseCode.SUCCESS, "消息已成功发往消息队列");

            // 处理聊天消息，生成序列号
            if (isChatMessage(request)) {
                request = fillChatMessageIds(request);
                response = buildChatMessageAckResponse(request);
            }

            // 发送消息到MQ
            MQMessageWrapper wrapper = buildMqMessageWrapper(request, metaData);
            SendResult sendResult = sendMessageToMQ(request, wrapper);

            if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                LOGGER.info("消息[{}]已推送消息队列[{}]", request.getMsgId(), wrapper.getDestination());
                return MessageBuilder.createImResponse(request, response);
            }

            LOGGER.error("消息[{}]推送消息队列[{}]失败:{}",
                    request.getMsgId(), wrapper.getDestination(), sendResult.getSendStatus());
            return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "消息发往消息队列失败");

        } catch (Exception e) {
            LOGGER.error("处理消息[{}]时发生异常", request.getMsgId(), e);
            return MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, "系统处理异常");
        }
    }

    @Override
    public void processOneway(ChannelHandlerContext ctx, ImMessage request) throws Exception {
        // 提取业务元数据
        MessageBusinessMeta metaData = ImMessageHelper.extractBusinessMQMeta(request);

        // 其他消息直接发往消息队列
        MQMessageWrapper wrapper = buildMqMessageWrapper(request, metaData);
        mqMessageSender.sendAsync(wrapper, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                LOGGER.info("消息[{}]推送消息队列[{}]", request.getMsgId(), wrapper.getDestination());
            }

            @Override
            public void onException(Throwable throwable) {
                LOGGER.error("消息[{}]推送消息队列[{}]失败:[{}]", request.getMsgId(), wrapper.getDestination(), throwable.getMessage());
            }
        });
    }


    private boolean isChatMessage(ImMessage message) {
        return message.getBodyCase() == ImMessage.BodyCase.CHAT_MESSAGE;
    }

    private ImMessage fillChatMessageIds(ImMessage message) {
        long messageId = snowflakeIdGenerator.nextId();
        long serverSeq = sequenceIdGenerator.nextConversationSeq(message.getChatMessage().getConversationId());
        return MessageUpdater.updateSequenceIds(message, messageId, serverSeq);
    }

    private Response buildChatMessageAckResponse(ImMessage updatedMessage) {
        ChatMessageAck chatMessageAck = ChatMessageAck.newBuilder()
                .setMessageId(updatedMessage.getChatMessage().getMessageId())
                .setServerSeq(updatedMessage.getChatMessage().getServerSeq())
                .setClientMsgId(updatedMessage.getChatMessage().getClientMsgId())
                .build();

        return MessageBuilder.createResponse(ResponseCode.SUCCESS, "消息已成功发往消息队列")
                .toBuilder()
                .setData(chatMessageAck.toByteString())
                .build();
    }

    private SendResult sendMessageToMQ(ImMessage message, MQMessageWrapper wrapper) {
        if (isChatMessage(message)) {
            return mqMessageSender.sendOrderly(wrapper,
                    String.valueOf(message.getChatMessage().getConversationId()));
        } else {
            return mqMessageSender.sendSync(wrapper);
        }
    }


    private MQMessageWrapper buildMqMessageWrapper(ImMessage message, MessageBusinessMeta meta) {
        MQMessageWrapper messageWrapper = new MQMessageWrapper();
        messageWrapper.setMessageType(meta.getMqMessageType());
        messageWrapper.setTargetTopic(meta.getTopic());
        messageWrapper.setTargetTag(meta.getTag());
        messageWrapper.setBody(message.toByteArray());
        messageWrapper.setSourceType(SourceType.USER);
        return messageWrapper;
    }
}
