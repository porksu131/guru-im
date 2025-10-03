package com.guru.im.offline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.offline.model.pojo.Message;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.protocol.util.MessageContent;
import com.guru.im.protocol.util.MessageParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MessageConverter {

    private static final Logger log = LoggerFactory.getLogger(MessageConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将数据库消息转换为ImMessage协议格式
     */
    public static ImMessage convertToImMessage(Message message) {

        return MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgType(ImMessage.MsgType.REQUEST)
                .setMessageType(com.guru.im.protocol.model.MessageType.CHAT) // 聊天消息
                .setChatMessage(convertToChatMessage(message))
                .build();
    }

    public static ChatMessage convertToChatMessage(Message message) {
        ChatMessage.Builder builder = ChatMessage.newBuilder();

        // 设置基础字段（带空值检查）
        if (message.getId() != null) builder.setMessageId(message.getId());
        if (message.getClientMsgId() != null) builder.setClientMsgId(message.getClientMsgId());
        if (message.getClientSendTime() != null) builder.setClientSendTime(message.getClientSendTime());
        if (message.getConversationType() != null) builder.setConversationTypeValue(message.getConversationType());
        if (message.getConversationId() != null) builder.setConversationId(message.getConversationId());
        if (message.getSenderId() != null) builder.setSenderId(message.getSenderId());
        if (message.getReceiverId() != null) builder.setReceiverId(message.getReceiverId());
        if (message.getMessageType() != null) builder.setChatMessageTypeValue(message.getMessageType());
        if (message.getServerSeq() != null) builder.setServerSeq(message.getServerSeq());
        if (message.getClientSeq() != null) builder.setClientSeq(message.getClientSeq());

        // 反序列化消息内容
        if (message.getMessageContent() != null) {
            try {
                Map<String, Object> contentMap = objectMapper.readValue(message.getMessageContent(), Map.class);
                setChatMessageContent(builder, contentMap, message.getMessageType());
            } catch (JsonProcessingException e) {
                log.error("消息内容反序列化失败: {}", message.getMessageContent(), e);
                throw new RuntimeException("消息内容解析失败");
            }
        }

        // 设置@用户列表
        if (message.getAtUsers() != null) {
            try {
                List<Long> atUsers = objectMapper.readValue(message.getAtUsers(), new TypeReference<List<Long>>() {
                });
                if (atUsers != null && !atUsers.isEmpty()) {
                    builder.addAllAtUsers(atUsers);
                }
            } catch (JsonProcessingException e) {
                log.warn("@用户列表反序列化失败: {}", message.getAtUsers(), e);
            }
        }

        return builder.build();
    }

    private static void setChatMessageContent(ChatMessage.Builder builder, Map<String, Object> contentMap, int messageType) {
        switch (messageType) {
            case ChatMessageType.TEXT_VALUE: // TEXT
                TextContent.Builder textBuilder = TextContent.newBuilder();
                if (contentMap.get("text") != null) {
                    textBuilder.setText((String) contentMap.get("text"));
                }
                if (contentMap.get("format") != null) {
                    try {
                        textBuilder.setFormat(TextContent.TextFormat.valueOf((String) contentMap.get("format")));
                    } catch (Exception e) {
                        log.warn("文本格式解析失败: {}", contentMap.get("format"));
                    }
                }
                builder.setTextContent(textBuilder.build());
                break;

            case ChatMessageType.IMAGE_VALUE: // IMAGE
                ImageContent.Builder imageBuilder = ImageContent.newBuilder();
                MediaContent.Builder mediaBuilder = getMediaContentBuilder(contentMap);

                imageBuilder.setBase(mediaBuilder.build());
                if (contentMap.get("width") != null) imageBuilder.setWidth(getInteger(contentMap, "width"));
                if (contentMap.get("height") != null) imageBuilder.setHeight(getInteger(contentMap, "height"));
                if (contentMap.get("format") != null) {
                    try {
                        imageBuilder.setFormat(ImageContent.ImageFormat.valueOf(getString(contentMap, "format")));
                    } catch (Exception e) {
                        log.warn("图片格式解析失败: {}", contentMap.get("format"));
                    }
                }
                builder.setImageContent(imageBuilder.build());
                break;

            case ChatMessageType.FILE_VALUE: // FILE
                FileContent.Builder fileBuilder = FileContent.newBuilder();
                MediaContent.Builder fileMediaBuilder = getMediaContentBuilder(contentMap);

                fileBuilder.setBase(fileMediaBuilder.build());
                if (contentMap.get("fileType") != null) {
                    try {
                        fileBuilder.setFileType(FileContent.FileType.valueOf(getString(contentMap, "fileType")));
                    } catch (Exception e) {
                        log.warn("文件类型解析失败: {}", contentMap.get("fileType"));
                    }
                }
                builder.setFileContent(fileBuilder.build());
                break;

            case ChatMessageType.VOICE_VALUE: // VOICE
                VoiceContent.Builder voiceBuilder = VoiceContent.newBuilder();
                MediaContent.Builder voiceMediaBuilder = getMediaContentBuilder(contentMap);

                voiceBuilder.setBase(voiceMediaBuilder.build());
                if (contentMap.get("duration") != null) voiceBuilder.setDuration(getLong(contentMap, "duration"));
                if (contentMap.get("codec") != null) voiceBuilder.setCodec(getString(contentMap, "codec"));
                if (contentMap.get("sampleRate") != null)
                    voiceBuilder.setSampleRate(getInteger(contentMap, "sampleRate"));
                builder.setVoiceContent(voiceBuilder.build());
                break;

            case ChatMessageType.VIDEO_VALUE: // VIDEO
                VideoContent.Builder videoBuilder = VideoContent.newBuilder();
                MediaContent.Builder videoMediaBuilder = getMediaContentBuilder(contentMap);

                videoBuilder.setBase(videoMediaBuilder.build());
                if (contentMap.get("duration") != null) videoBuilder.setDuration(getLong(contentMap, "duration"));
                if (contentMap.get("width") != null) videoBuilder.setWidth(getInteger(contentMap, "width"));
                if (contentMap.get("height") != null) videoBuilder.setHeight(getInteger(contentMap, "height"));
                if (contentMap.get("audioCodec") != null)
                    videoBuilder.setAudioCodec(getInteger(contentMap, "audioCodec"));
                if (contentMap.get("audioBitrate") != null)
                    videoBuilder.setAudioBitrate(getInteger(contentMap, "audioBitrate"));
                if (contentMap.get("videoCodec") != null)
                    videoBuilder.setVideoCodec(getInteger(contentMap, "videoCodec"));
                if (contentMap.get("videoBitrate") != null)
                    videoBuilder.setVideoBitrate(getInteger(contentMap, "videoBitrate"));
                builder.setVideoContent(videoBuilder.build());
                break;

            case ChatMessageType.LOCATION_VALUE: // LOCATION
                LocationContent.Builder locationBuilder = LocationContent.newBuilder();
                if (contentMap.get("latitude") != null)
                    locationBuilder.setLatitude(getDouble(contentMap, "latitude"));
                if (contentMap.get("longitude") != null)
                    locationBuilder.setLongitude(getDouble(contentMap, "longitude"));
                if (contentMap.get("title") != null) locationBuilder.setTitle(getString(contentMap, "title"));
                if (contentMap.get("address") != null) locationBuilder.setAddress(getString(contentMap, "address"));
                if (contentMap.get("zoom") != null) locationBuilder.setZoom(getInteger(contentMap, "zoom"));
                builder.setLocationContent(locationBuilder.build());
                break;

            default:
                log.warn("未知消息类型: {}", messageType);
        }
    }

    private static MediaContent.Builder getMediaContentBuilder(Map<String, Object> contentMap) {
        MediaContent.Builder fileMediaBuilder = MediaContent.newBuilder();

        fileMediaBuilder.setFileId(getLong(contentMap, "fileId"));
        fileMediaBuilder.setFileName(getString(contentMap, "fileName"));
        fileMediaBuilder.setFileUrl(getString(contentMap, "fileUrl"));
        fileMediaBuilder.setThumbnailUrl(getString(contentMap, "thumbnailUrl"));
        fileMediaBuilder.setFileSize(getLong(contentMap, "fileSize"));
        fileMediaBuilder.setMimeType(getString(contentMap, "mimeType"));

        return fileMediaBuilder;
    }

    // ==================== 类型安全的getter方法 ====================

    private static Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Long, key: {}, value: {}", key, value);
            return null;
        }
    }

    private static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Integer, key: {}, value: {}", key, value);
            return null;
        }
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Double, key: {}, value: {}", key, value);
            return null;
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    public static Message convertToEntity(ChatMessage chatMessage) {
        Message entity = new Message();

        // 设置基础字段
        entity.setId(chatMessage.getMessageId());
        entity.setClientMsgId(chatMessage.getClientMsgId());
        entity.setClientSendTime(chatMessage.getClientSendTime());
        entity.setConversationType(chatMessage.getConversationTypeValue());
        entity.setConversationId(chatMessage.getConversationId());
        entity.setSenderId(chatMessage.getSenderId());
        entity.setReceiverId(chatMessage.getReceiverId());
        entity.setMessageType(chatMessage.getChatMessageTypeValue());

        // 序列化消息内容为JSON
        try {
            MessageContent messageContent = MessageParserHelper.parseChatMessageContent(chatMessage);
            String jsonContent = objectMapper.writeValueAsString(messageContent.getContent());
            entity.setMessageContent(jsonContent);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("消息内容序列化失败");
        }

        // 设置其他字段
        entity.setServerSeq(chatMessage.getServerSeq());
        entity.setClientSeq(chatMessage.getClientSeq());
        entity.setAtUsers(serializeAtUsers(chatMessage.getAtUsersList()));
        entity.setRecalled(false);
        entity.setReadCount(0);
        entity.setStatus(1);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setUpdateTime(System.currentTimeMillis());

        return entity;
    }

    private static String serializeAtUsers(List<Long> atUsers) {
        if (atUsers == null || atUsers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(atUsers);
        } catch (JsonProcessingException e) {
            log.warn("@用户列表序列化失败", e);
            return null;
        }
    }
}