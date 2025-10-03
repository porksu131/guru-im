package com.guru.im.protocol.util;

import com.guru.im.protocol.model.*;

import java.util.*;

public class MessageParserHelper {

    public static String exactMessageSummary(ChatMessage chatMessage) {
        switch (chatMessage.getContentCase()) {
            case TEXT_CONTENT:
                TextContent textContent = chatMessage.getTextContent();
                return textContent.getText().length() > 50 ?
                        textContent.getText().substring(0, 50) + "..." : textContent.getText();

            case IMAGE_CONTENT:
                return "[图片]";

            case FILE_CONTENT:
                FileContent fileContent = chatMessage.getFileContent();
                return "[文件] " + fileContent.getBase().getFileName();

            case VOICE_CONTENT:
                return "[语音]";

            case VIDEO_CONTENT:
                return "[视频]";

            case LOCATION_CONTENT:
                return "[位置]";

            default:
                return "[新消息]";
        }
    }

    /**
     * 解析ImMessage并提取结构化数据
     */
    public static MessageContent parseChatMessageContent(ChatMessage chatMessage) {
        MessageContent content = new MessageContent();
        content.setMessageType(chatMessage.getChatMessageType());
        switch (chatMessage.getContentCase()) {
            case TEXT_CONTENT:
                TextContent textContent = chatMessage.getTextContent();
                content.setContent(parseTextContent(textContent));
                break;

            case IMAGE_CONTENT:
                ImageContent imageContent = chatMessage.getImageContent();
                content.setContent(parseImageContent(imageContent));
                break;

            case FILE_CONTENT:
                FileContent fileContent = chatMessage.getFileContent();
                content.setContent(parseFileContent(fileContent));
                break;

            case VOICE_CONTENT:
                VoiceContent voiceContent = chatMessage.getVoiceContent();
                content.setContent(parseVoiceContent(voiceContent));
                break;

            case VIDEO_CONTENT:
                VideoContent videoContent = chatMessage.getVideoContent();
                content.setContent(parseVideoContent(videoContent));
                break;

            case LOCATION_CONTENT:
                LocationContent locationContent = chatMessage.getLocationContent();
                content.setContent(parseLocationContent(locationContent));
                break;

            default:
                //log.warn("未知消息类型: {}", imMessage.getMessageType());
                content.setContent(Collections.singletonMap("raw", "未知消息类型"));
        }

        return content;
    }

    private static Map<String, Object> parseVideoContent(VideoContent videoContent) {
        MediaContent media = videoContent.getBase();
        Map<String, Object> contentMap = parseMediaContentBase(media);
        contentMap.put("duration", videoContent.getDuration());
        contentMap.put("width", videoContent.getWidth());
        contentMap.put("height", videoContent.getHeight());
        contentMap.put("audioCodec", videoContent.getAudioCodec());
        contentMap.put("audioBitrate", videoContent.getAudioBitrate());
        contentMap.put("videoCodec", videoContent.getVideoCodec());
        contentMap.put("videoBitrate", videoContent.getVideoBitrate());
        return contentMap;
    }

    private static Map<String, Object> parseVoiceContent(VoiceContent voiceContent) {
        MediaContent media = voiceContent.getBase();
        Map<String, Object> contentMap = parseMediaContentBase(media);
        contentMap.put("duration", voiceContent.getDuration());
        contentMap.put("codec", voiceContent.getCodec());
        contentMap.put("sampleRate", voiceContent.getSampleRate());
        return contentMap;
    }

    private static Map<String, Object> parseTextContent(TextContent textContent) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("text", textContent.getText());
        contentMap.put("format", textContent.getFormat().name());

        if (!textContent.getSegmentsList().isEmpty()) {
            List<Map<String, Object>> segments = new ArrayList<>();
            for (TextContent.TextSegment segment : textContent.getSegmentsList()) {
                Map<String, Object> segmentMap = new HashMap<>();
                segmentMap.put("text", segment.getText());
                segmentMap.put("style", parseTextStyle(segment.getStyle()));
                segments.add(segmentMap);
            }
            contentMap.put("segments", segments);
        }

        return contentMap;
    }

    private static Map<String, Object> parseImageContent(ImageContent imageContent) {
        MediaContent media = imageContent.getBase();
        Map<String, Object> contentMap = parseMediaContentBase(media);
        contentMap.put("width", imageContent.getWidth());
        contentMap.put("height", imageContent.getHeight());
        contentMap.put("format", imageContent.getFormat().name());

        return contentMap;
    }

    private static Map<String, Object> parseFileContent(FileContent fileContent) {
        MediaContent media = fileContent.getBase();
        Map<String, Object> contentMap = parseMediaContentBase(media);
        contentMap.put("fileType", fileContent.getFileType().name());
        return contentMap;
    }

    private static Map<String, Object> parseLocationContent(LocationContent locationContent) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("latitude", locationContent.getLatitude());
        contentMap.put("longitude", locationContent.getLongitude());
        contentMap.put("title", locationContent.getTitle());
        contentMap.put("address", locationContent.getAddress());
        contentMap.put("zoom", locationContent.getZoom());
        return contentMap;
    }

    private static Map<String, Object> parseTextStyle(TextContent.TextSegment.TextStyle textStyle) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("bold", textStyle.getBold());
        contentMap.put("italic", textStyle.getItalic());
        contentMap.put("color", textStyle.getColor());
        contentMap.put("font", textStyle.getFont());
        contentMap.put("size", textStyle.getSize());
        return contentMap;
    }

    private static Map<String, Object> parseMediaContentBase(MediaContent media) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("fileId", media.getFileId());
        contentMap.put("fileName", media.getFileName());
        contentMap.put("fileUrl", media.getFileUrl());
        contentMap.put("thumbnailUrl", media.getThumbnailUrl());
        contentMap.put("fileSize", media.getFileSize());
        contentMap.put("mimeType", media.getMimeType());

        return contentMap;
    }

    // 其他类型解析方法类似...
}