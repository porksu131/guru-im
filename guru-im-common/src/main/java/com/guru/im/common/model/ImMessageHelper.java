package com.guru.im.common.model;

import com.guru.im.common.constant.MQTag;
import com.guru.im.common.constant.MQTopic;
import com.guru.im.protocol.model.ImMessage;

public class ImMessageHelper {
//
//    // 统一的业务元数据提取器接口
//    @FunctionalInterface
//    private interface MetaExtractor<T> {
//        MessageBusinessMeta extract(T message, MessageEventType messageEventType);
//    }
//
//    // 批量消息提取器接口
//    @FunctionalInterface
//    private interface BatchMetaExtractor<T> {
//        MessageBusinessMeta extract(long receiverId, T message, MessageEventType messageEventType);
//    }
//
//    // 注册所有单消息提取器
//    private static final Map<Class<?>, MetaExtractor<?>> extractors = new HashMap<>();
//
//    // 注册所有批量消息提取器
//    private static final Map<Class<?>, BatchMetaExtractor<?>> batchExtractors = new HashMap<>();
//
//    static {
//        // 初始化单消息提取器
//        initSingleExtractors();
//        // 初始化批量消息提取器
//        initBatchExtractors();
//    }
//
//    private static void initSingleExtractors() {
//        // 聊天消息提取器 - 使用具体的类型
//        extractors.put(TextContent.class, (MetaExtractor<TextContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(EmojiContent.class, (MetaExtractor<EmojiContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(ImageContent.class, (MetaExtractor<ImageContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(VoiceContent.class, (MetaExtractor<VoiceContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(VideoContent.class, (MetaExtractor<VideoContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(FileContent.class, (MetaExtractor<FileContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//        extractors.put(LocationContent.class, (MetaExtractor<LocationContent>) (message, eventType) ->
//                extractChatMeta(message.getClass().getName(), message.getHeader(), message.toByteArray(), eventType));
//
//        // 特殊消息提取器
//        extractors.put(PresenceNotify.class, (MetaExtractor<PresenceNotify>) ImMessageHelper::extractPresenceNotifyMeta);
//        extractors.put(FriendRequest.class, (MetaExtractor<FriendRequest>) ImMessageHelper::extractFriendRequestMeta);
//    }
//
//    private static void initBatchExtractors() {
//        // 批量聊天消息提取器
//        batchExtractors.put(BatchTextContent.class, (BatchMetaExtractor<BatchTextContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//
//        batchExtractors.put(BatchEmojiContent.class, (BatchMetaExtractor<BatchEmojiContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        batchExtractors.put(BatchImageContent.class, (BatchMetaExtractor<BatchImageContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        batchExtractors.put(BatchVoiceContent.class, (BatchMetaExtractor<BatchVoiceContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        batchExtractors.put(BatchVideoContent.class, (BatchMetaExtractor<BatchVideoContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        batchExtractors.put(BatchFileContent.class, (BatchMetaExtractor<BatchFileContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        batchExtractors.put(BatchLocationContent.class, (BatchMetaExtractor<BatchLocationContent>) (receiverId, message, eventType) ->
//        {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(item -> item.getHeader().getMessageId()).toList(),
//                    eventType);
//            return extractBatchChatMeta(receiverId, message.getClass().getName(), bytes, eventType);
//        });
//
//        // 批量特殊消息提取器
//        batchExtractors.put(BatchPresenceNotify.class, (BatchMetaExtractor<BatchPresenceNotify>) (receiverId, message, eventType) -> {
//            if (eventType.equals(MessageEventType.REQUEST_FROM_MICRO_SERVICE)) {
//                byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                        () -> message.getItemsList().stream().map(PresenceNotify::getReceiverId).toList(),
//                        eventType);
//                return createControlMeta(message.getClass().getName(),
//                        receiverId, true,
//                        MqEventType.DISPATCH_MESSAGE_ONEWAY,
//                        MQTopic.DISPATCH_CONTROL_TOPIC, bytes);
//            }
//            return null;
//        });
//
//        batchExtractors.put(BatchFriendRequest.class, (BatchMetaExtractor<BatchFriendRequest>) (receiverId, message, eventType) -> {
//            byte[] bytes = BatchResponseBytes(message.getClass().getName(),
//                    () -> message.getItemsList().stream().map(FriendRequest::getReceiverId).toList(),
//                    eventType);
//            return createNotifyMeta(message.getClass().getName(), receiverId, true, eventType, bytes);
//        });
//    }
//
//    public static MessageBusinessMeta extractBusinessMeta(ImMessage imMessage, MessageEventType messageEventType) {
//        return switch (imMessage.getBodyCase()) {
//            case EMOJI_CONTENT -> extractWithExtractor(imMessage.getEmojiContent(), messageEventType);
//            case TEXT_CONTENT -> extractWithExtractor(imMessage.getTextContent(), messageEventType);
//            case IMAGE_CONTENT -> extractWithExtractor(imMessage.getImageContent(), messageEventType);
//            case VOICE_CONTENT -> extractWithExtractor(imMessage.getVoiceContent(), messageEventType);
//            case VIDEO_CONTENT -> extractWithExtractor(imMessage.getVideoContent(), messageEventType);
//            case LOCATION_CONTENT -> extractWithExtractor(imMessage.getLocationContent(), messageEventType);
//            case FILE_CONTENT -> extractWithExtractor(imMessage.getFileContent(), messageEventType);
//            case PRESENCE_NOTIFY -> extractWithExtractor(imMessage.getPresenceNotify(), messageEventType);
//            case FRIEND_REQUEST -> extractWithExtractor(imMessage.getFriendRequest(), messageEventType);
//            case BATCH_MESSAGE -> extractBusinessMeta(imMessage.getBatchMessage(), messageEventType);
//            default ->
//                    throw new IllegalArgumentException("getHeader failed, Unsupported im message body: " + imMessage.getBodyCase());
//        };
//    }
//
//    public static MessageBusinessMeta extractBusinessMeta(BatchMessage batchMessage, MessageEventType messageEventType) {
//        return switch (batchMessage.getBatchContentCase()) {
//            case BATCH_EMOJI_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchEmojiContent(), messageEventType);
//            case BATCH_TEXT_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchTextContent(), messageEventType);
//            case BATCH_IMAGE_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchImageContent(), messageEventType);
//            case BATCH_VOICE_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchVoiceContent(), messageEventType);
//            case BATCH_VIDEO_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchVideoContent(), messageEventType);
//            case BATCH_LOCATION_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchLocationContent(), messageEventType);
//            case BATCH_FILE_CONTENT ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchFileContent(), messageEventType);
//            case BATCH_PRESENCE_NOTIFY ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchPresenceNotify(), messageEventType);
//            case BATCH_FRIEND_REQUEST ->
//                    extractWithBatchExtractor(batchMessage.getReceiverId(), batchMessage.getBatchFriendRequest(), messageEventType);
//            default ->
//                    throw new IllegalArgumentException("getHeader failed, Unsupported batch message body: " + batchMessage.getBatchContentCase());
//        };
//    }
//
//    @SuppressWarnings("unchecked")
//    private static <T> MessageBusinessMeta extractWithExtractor(T message, MessageEventType messageEventType) {
//        MetaExtractor<T> extractor = (MetaExtractor<T>) extractors.get(message.getClass());
//        if (extractor != null) {
//            return extractor.extract(message, messageEventType);
//        }
//        throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
//    }
//
//    @SuppressWarnings("unchecked")
//    private static <T> MessageBusinessMeta extractWithBatchExtractor(long receiverId, T message, MessageEventType messageEventType) {
//        BatchMetaExtractor<T> extractor = (BatchMetaExtractor<T>) batchExtractors.get(message.getClass());
//        if (extractor != null) {
//            return extractor.extract(receiverId, message, messageEventType);
//        }
//        throw new IllegalArgumentException("Unsupported batch message type: " + message.getClass());
//    }
//
//    // 特殊消息的提取方法
//    private static MessageBusinessMeta extractPresenceNotifyMeta(PresenceNotify presenceNotify, MessageEventType messageEventType) {
//        if (messageEventType.equals(MessageEventType.REQUEST_FROM_MICRO_SERVICE)) {
//            return createControlMeta(presenceNotify.getClass().getName(),
//                    presenceNotify.getReceiverId(), false,
//                    MqEventType.DISPATCH_MESSAGE_ONEWAY,
//                    MQTopic.DISPATCH_CONTROL_TOPIC, presenceNotify.toByteArray());
//        }
//        return null;
//    }
//
//    private static MessageBusinessMeta extractFriendRequestMeta(FriendRequest friendRequest, MessageEventType messageEventType) {
//        return createNotifyMeta(friendRequest.getClass().getName(),
//                friendRequest.getReceiverId(), false,
//                messageEventType, friendRequest.toByteArray());
//    }
//
//    // 统一的元数据创建方法
//    private static MessageBusinessMeta createBaseMeta(String messageType, long receiverId,
//                                                      boolean isBatch, String business, byte[] data) {
//        MessageBusinessMeta meta = new MessageBusinessMeta();
//        meta.setMessageType(messageType);
//        meta.setBatch(isBatch);
//        meta.setReceiverId(receiverId);
//        meta.setBusiness(business);
//        meta.setData(data);
//        return meta;
//    }
//
//    private static MessageBusinessMeta createControlMeta(String messageType, long receiverId,
//                                                         boolean isBatch, int eventType, String topic, byte[] data) {
//        MessageBusinessMeta meta = createBaseMeta(messageType, receiverId, isBatch, Business.CONTROL, data);
//        meta.setEventType(eventType);
//        meta.setTopic(topic);
//        return meta;
//    }
//
//    private static MessageBusinessMeta createNotifyMeta(String messageType, long receiverId,
//                                                        boolean isBatch, MessageEventType messageEventType, byte[] data) {
//        MessageBusinessMeta meta = createBaseMeta(messageType, receiverId, isBatch, Business.NOTIFY, data);
//
//        switch (messageEventType) {
//            case REQUEST_FROM_MICRO_SERVICE:
//                meta.setEventType(MqEventType.DISPATCH_MESSAGE);
//                meta.setTopic(MQTopic.DISPATCH_NOTIFY_TOPIC);
//                break;
//            case RESPONSE_FROM_DISPATCH_SUCCESS:
//                meta.setEventType(MqEventType.DISPATCH_SUCCESS);
//                meta.setTopic(MQTopic.USR_TOPIC);
//                break;
//            case RESPONSE_FROM_DISPATCH_FAILURE:
//                meta.setEventType(MqEventType.DISPATCH_FAIL);
//                meta.setTopic(MQTopic.OFFLINE_TOPIC);
//                break;
//            default:
//                // 不做处理
//                break;
//        }
//        return meta;
//    }
//
//    private static MessageBusinessMeta extractChatMeta(String classType, MessageHeader messageHeader, byte[] data, MessageEventType messageEventType) {
//        MessageBusinessMeta meta = createBaseMeta(classType, messageHeader.getReceiverId(), false, Business.CHAT, data);
//        return configureChatEventType(meta, messageEventType, messageHeader);
//    }
//
//    private static MessageBusinessMeta extractBatchChatMeta(long receiverId, String classType, byte[] data, MessageEventType messageEventType) {
//        MessageBusinessMeta meta = createBaseMeta(classType, receiverId, true, Business.CHAT, data);
//        return configureChatEventType(meta, messageEventType, null);
//    }
//
//    private static MessageBusinessMeta configureChatEventType(MessageBusinessMeta meta,
//                                                              MessageEventType eventType,
//                                                              MessageHeader header) {
//        switch (eventType) {
//            case REQUEST_FROM_USER:
//                meta.setEventType(MqEventType.CREATE_MESSAGE);
//                meta.setTopic(getChatTopic(header));
//                break;
//            case REQUEST_FROM_MICRO_SERVICE:
//                meta.setEventType(MqEventType.CREATE_MESSAGE);
//                meta.setTopic(MQTopic.DISPATCH_MSG_TOPIC);
//                break;
//            case RESPONSE_FROM_DISPATCH_SUCCESS:
//                meta.setEventType(MqEventType.DISPATCH_SUCCESS);
//                meta.setTopic(MQTopic.SING_CHAT_TOPIC);
//                break;
//            case RESPONSE_FROM_DISPATCH_FAILURE:
//                meta.setEventType(MqEventType.DISPATCH_FAIL);
//                meta.setTopic(MQTopic.OFFLINE_TOPIC);
//                break;
//            default:
//                // 不做处理
//                break;
//        }
//        return meta;
//    }
//
//    private static String getChatTopic(MessageHeader header) {
//        if (header != null && ConversationType.PRIVATE == header.getConversationType()) {
//            return MQTopic.SING_CHAT_TOPIC + ":chat-event";
//        }
//        if (header != null && ConversationType.GROUP == header.getConversationType()) {
//            return MQTopic.GROUP_CHAT_TOPIC + ":chat-event";
//        }
//        return MQTopic.SING_CHAT_TOPIC + ":chat-event"; // 默认值
//    }
//
//    private static byte[] BatchResponseBytes(String classType, Supplier<List<Long>> batchIds, MessageEventType messageEventType) {
//        if (messageEventType == MessageEventType.RESPONSE_FROM_DISPATCH_SUCCESS
//                || messageEventType == MessageEventType.RESPONSE_FROM_DISPATCH_FAILURE) {
//            return BatchResponse.newBuilder()
//                    .setBatchContentType(classType)
//                    .addAllItems(batchIds.get())
//                    .build().toByteArray();
//        }
//        return null;
//    }

    public static MessageBusinessMeta extractBusinessMQMeta(ImMessage imMessage) {
        switch (imMessage.getBodyCase()) {
            case CHAT_MESSAGE:
                return buildMQMeta(MQTopic.SING_CHAT_TOPIC, MQTag.CHAT, MQMessageType.CHAT);
            case PRESENCE_NOTIFY:
                return buildMQMeta(MQTopic.USR_TOPIC, MQTag.ACTION, MQMessageType.ACTION);
            case SYNC_REQUEST:
            case SYNC_EVENT_REQUEST:
                return buildMQMeta(MQTopic.OFFLINE_TOPIC, MQTag.BATCH_SYNC, MQMessageType.BATCH_SYNC);
            case SYNC_ACK:
                return buildMQMeta(MQTopic.OFFLINE_TOPIC, MQTag.BATCH_ACK, MQMessageType.BATCH_ACK);
            case READ_RECEIPT_REQ:
                return buildMQMeta(MQTopic.SING_CHAT_TOPIC, MQTag.ACTION, MQMessageType.ACTION);
            case SIGNALING_MESSAGE:
                return buildMQMeta(MQTopic.SIGNAL_TOPIC, MQTag.SIGNAL, MQMessageType.SIGNAL);
            default:
                throw new IllegalArgumentException("Unsupported im message type: " + imMessage.getBodyCase());
        }
    }

    private static MessageBusinessMeta buildMQMeta(String topic, String tag, MQMessageType mqMessageType) {
        MessageBusinessMeta meta = new MessageBusinessMeta();
        meta.setTopic(topic);
        meta.setTag(tag);
        meta.setMqMessageType(mqMessageType);
        return meta;
    }
}