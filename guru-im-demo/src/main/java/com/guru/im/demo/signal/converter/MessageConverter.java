package com.guru.im.demo.signal.converter;

import com.guru.im.protocol.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Protobuf消息和JSON消息之间的转换器
 */
public class MessageConverter {
    private static final Gson gson = new Gson();
    
    /**
     * 将JSON转换为Protobuf SignalingMessage
     */
    public static SignalingMessage jsonToSignalingMessage(JsonObject json) {
        try {
            SignalingMessage.Builder builder = SignalingMessage.newBuilder();
            
            if (json.has("message_id")) {
                builder.setMessageId(json.get("message_id").getAsLong());
            }
            if (json.has("session_id")) {
                builder.setSessionId(json.get("session_id").getAsLong());
            }
            if (json.has("from_user")) {
                builder.setFromUser(json.get("from_user").getAsLong());
            }
            if (json.has("from_device")) {
                builder.setFromDevice(json.get("from_device").getAsString());
            }
            if (json.has("type")) {
                String typeStr = json.get("type").getAsString();
                try {
                    SignalingType type = SignalingType.valueOf(typeStr);
                    builder.setType(type);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("未知的信令类型: " + typeStr);
                }
            }
            if (json.has("timestamp")) {
                builder.setTimestamp(json.get("timestamp").getAsLong());
            }
            if (json.has("version")) {
                builder.setVersion(json.get("version").getAsInt());
            }
            
            // 处理to_users
            if (json.has("to_users")) {
                JsonObject toUsers = json.getAsJsonObject("to_users");
                toUsers.entrySet().forEach(entry -> {
                    builder.addToUsers(entry.getValue().getAsLong());
                });
            }

            // 处理payload
            if (json.has("payload")) {
                JsonObject payload = json.getAsJsonObject("payload");

                // === 通话相关payload ===
                if (payload.has("call_request")) {
                    CallRequest callRequest = gson.fromJson(payload.get("call_request"), CallRequest.class);
                    builder.setCallRequest(callRequest);
                } else if (payload.has("call_response")) {
                    CallResponse callResponse = gson.fromJson(payload.get("call_response"), CallResponse.class);
                    builder.setCallResponse(callResponse);
                } else if (payload.has("hangup")) {
                    CallHangup hangup = gson.fromJson(payload.get("hangup"), CallHangup.class);
                    builder.setHangup(hangup);
                } else if (payload.has("state_sync")) {
                    CallStateSync stateSync = gson.fromJson(payload.get("state_sync"), CallStateSync.class);
                    builder.setStateSync(stateSync);
                } else if (payload.has("ringing")) {
                    Ringing ringing = gson.fromJson(payload.get("ringing"), Ringing.class);
                    builder.setRinging(ringing);
                } else if (payload.has("call_request_response")) {
                    CallRequestResponse callRequestResponse = gson.fromJson(payload.get("call_request_response"), CallRequestResponse.class);
                    builder.setCallRequestResponse(callRequestResponse);
                }

                // === 会议相关payload ===
                else if (payload.has("conference_invite")) {
                    ConferenceInvite conferenceInvite = gson.fromJson(payload.get("conference_invite"), ConferenceInvite.class);
                    builder.setConferenceInvite(conferenceInvite);
                } else if (payload.has("conference_join")) {
                    ConferenceJoin conferenceJoin = gson.fromJson(payload.get("conference_join"), ConferenceJoin.class);
                    builder.setConferenceJoin(conferenceJoin);
                } else if (payload.has("conference_leave")) {
                    ConferenceLeave conferenceLeave = gson.fromJson(payload.get("conference_leave"), ConferenceLeave.class);
                    builder.setConferenceLeave(conferenceLeave);
                } else if (payload.has("conference_kick")) {
                    ConferenceKick conferenceKick = gson.fromJson(payload.get("conference_kick"), ConferenceKick.class);
                    builder.setConferenceKick(conferenceKick);
                } else if (payload.has("conference_update")) {
                    ConferenceUpdate conferenceUpdate = gson.fromJson(payload.get("conference_update"), ConferenceUpdate.class);
                    builder.setConferenceUpdate(conferenceUpdate);
                }else if (payload.has("conference_invite_response")) {
                    ConferenceInviteResponse conferenceInviteResponse = gson.fromJson(payload.get("conference_invite_response"), ConferenceInviteResponse.class);
                    builder.setConferenceInviteResponse(conferenceInviteResponse);
                }

                // === 媒体控制payload ===
                else if (payload.has("media_control")) {
                    MediaControl mediaControl = gson.fromJson(payload.get("media_control"), MediaControl.class);
                    builder.setMediaControl(mediaControl);
                }

                // === mediasoup相关payload处理 ===

                // 房间信息相关
                else if (payload.has("rtp_capabilities_request")) {
                    RtpCapabilitiesRequest request = gson.fromJson(
                            payload.get("rtp_capabilities_request"), RtpCapabilitiesRequest.class);
                    builder.setRtpCapabilitiesRequest(request);
                } else if (payload.has("rtp_capabilities_response")) {
                    RtpCapabilitiesResponse response = gson.fromJson(
                            payload.get("rtp_capabilities_response"), RtpCapabilitiesResponse.class);
                    builder.setRtpCapabilitiesResponse(response);
                } else if (payload.has("room_producers_request")) {
                    RoomProducersRequest request = gson.fromJson(
                            payload.get("room_producers_request"), RoomProducersRequest.class);
                    builder.setRoomProducersRequest(request);
                } else if (payload.has("room_producers_response")) {
                    RoomProducersResponse response = gson.fromJson(
                            payload.get("room_producers_response"), RoomProducersResponse.class);
                    builder.setRoomProducersResponse(response);
                }

                // 传输相关
                else if (payload.has("transport_create")) {
                    MediasoupTransportCreate transportCreate = gson.fromJson(
                            payload.get("transport_create"), MediasoupTransportCreate.class);
                    builder.setTransportCreate(transportCreate);
                } else if (payload.has("transport_connect")) {
                    MediasoupTransportConnect transportConnect = gson.fromJson(
                            payload.get("transport_connect"), MediasoupTransportConnect.class);
                    builder.setTransportConnect(transportConnect);
                } else if (payload.has("transport_connect_response")) {
                    MediasoupTransportConnectResponse transportConnectResponse = gson.fromJson(
                            payload.get("transport_connect_response"), MediasoupTransportConnectResponse.class);
                    builder.setTransportConnectResponse(transportConnectResponse);
                }

                // 生产者相关
                else if (payload.has("produce")) {
                    MediasoupProduce produce = gson.fromJson(
                            payload.get("produce"), MediasoupProduce.class);
                    builder.setProduce(produce);
                } else if (payload.has("produce_response")) {
                    MediasoupProduceResponse produceResponse = gson.fromJson(
                            payload.get("produce_response"), MediasoupProduceResponse.class);
                    builder.setProduceResponse(produceResponse);
                }

                // 消费者相关
                else if (payload.has("consume")) {
                    MediasoupConsume consume = gson.fromJson(
                            payload.get("consume"), MediasoupConsume.class);
                    builder.setConsume(consume);
                } else if (payload.has("consume_response")) {
                    MediasoupConsumeResponse consumeResponse = gson.fromJson(
                            payload.get("consume_response"), MediasoupConsumeResponse.class);
                    builder.setConsumeResponse(consumeResponse);
                } else if (payload.has("consumer_resume")) {
                    MediasoupConsumerResume consumerResume = gson.fromJson(
                            payload.get("consumer_resume"), MediasoupConsumerResume.class);
                    builder.setConsumerResume(consumerResume);
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("JSON转换为SignalingMessage失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将Protobuf SignalingMessage转换为JSON
     */
    public static String signalingMessageToJson(SignalingMessage message) {
        try {
            JsonObject json = new JsonObject();
            
            json.addProperty("message_id", message.getMessageId());
            json.addProperty("session_id", message.getSessionId());
            json.addProperty("from_user", message.getFromUser());
            json.addProperty("from_device", message.getFromDevice());
            json.addProperty("type", message.getType().name());
            json.addProperty("timestamp", message.getTimestamp());
            json.addProperty("version", message.getVersion());
            
            // 处理to_users
            JsonObject toUsersJson = new JsonObject();
            for (int i = 0; i < message.getToUsersCount(); i++) {
                toUsersJson.addProperty(String.valueOf(i), message.getToUsers(i));
            }
            json.add("to_users", toUsersJson);

            // 处理payload
            JsonObject payloadJson = new JsonObject();
            switch (message.getPayloadCase()) {
                // === 通话相关payload ===
                case CALL_REQUEST:
                    payloadJson.add("call_request", gson.toJsonTree(message.getCallRequest()));
                    break;
                case CALL_RESPONSE:
                    payloadJson.add("call_response", gson.toJsonTree(message.getCallResponse()));
                    break;
                case HANGUP:
                    payloadJson.add("hangup", gson.toJsonTree(message.getHangup()));
                    break;
                case STATE_SYNC:
                    payloadJson.add("state_sync", gson.toJsonTree(message.getStateSync()));
                    break;
                case RINGING:
                    payloadJson.add("ringing", gson.toJsonTree(message.getRinging()));
                    break;
                case CALL_REQUEST_RESPONSE:
                    payloadJson.add("call_request_response", gson.toJsonTree(message.getCallRequestResponse()));
                    break;

                // === 会议相关payload ===
                case CONFERENCE_INVITE:
                    payloadJson.add("conference_invite", gson.toJsonTree(message.getConferenceInvite()));
                    break;
                case CONFERENCE_JOIN:
                    payloadJson.add("conference_join", gson.toJsonTree(message.getConferenceJoin()));
                    break;
                case CONFERENCE_LEAVE:
                    payloadJson.add("conference_leave", gson.toJsonTree(message.getConferenceLeave()));
                    break;
                case CONFERENCE_KICK:
                    payloadJson.add("conference_kick", gson.toJsonTree(message.getConferenceKick()));
                    break;
                case CONFERENCE_UPDATE:
                    payloadJson.add("conference_update", gson.toJsonTree(message.getConferenceUpdate()));
                    break;
                case CONFERENCE_INVITE_RESPONSE:
                    payloadJson.add("conference_invite_response", gson.toJsonTree(message.getConferenceInviteResponse()));
                    break;

                // === 媒体控制payload ===
                case MEDIA_CONTROL:
                    payloadJson.add("media_control", gson.toJsonTree(message.getMediaControl()));
                    break;

                // === mediasoup相关payload ===

                // 房间信息相关
                case RTP_CAPABILITIES_REQUEST:
                    payloadJson.add("rtp_capabilities_request", gson.toJsonTree(message.getRtpCapabilitiesRequest()));
                    break;
                case RTP_CAPABILITIES_RESPONSE:
                    payloadJson.add("rtp_capabilities_response", gson.toJsonTree(message.getRtpCapabilitiesResponse()));
                    break;
                case ROOM_PRODUCERS_REQUEST:
                    payloadJson.add("room_producers_request", gson.toJsonTree(message.getRoomProducersRequest()));
                    break;
                case ROOM_PRODUCERS_RESPONSE:
                    payloadJson.add("room_producers_response", gson.toJsonTree(message.getRoomProducersResponse()));
                    break;

                // 传输相关
                case TRANSPORT_CREATE:
                    payloadJson.add("transport_create", gson.toJsonTree(message.getTransportCreate()));
                    break;
                case TRANSPORT_CONNECT:
                    payloadJson.add("transport_connect", gson.toJsonTree(message.getTransportConnect()));
                    break;
                case TRANSPORT_CONNECT_RESPONSE:
                    payloadJson.add("transport_connect_response", gson.toJsonTree(message.getTransportConnectResponse()));
                    break;

                // 生产者相关
                case PRODUCE:
                    payloadJson.add("produce", gson.toJsonTree(message.getProduce()));
                    break;
                case PRODUCE_RESPONSE:
                    payloadJson.add("produce_response", gson.toJsonTree(message.getProduceResponse()));
                    break;

                // 消费者相关
                case CONSUME:
                    payloadJson.add("consume", gson.toJsonTree(message.getConsume()));
                    break;
                case CONSUME_RESPONSE:
                    payloadJson.add("consume_response", gson.toJsonTree(message.getConsumeResponse()));
                    break;
                case CONSUMER_RESUME:
                    payloadJson.add("consumer_resume", gson.toJsonTree(message.getConsumerResume()));
                    break;

                case PAYLOAD_NOT_SET:
                    // 无payload
                    break;
            }
            
            if (payloadJson.size() > 0) {
                json.add("payload", payloadJson);
            }
            
            return gson.toJson(json);
        } catch (Exception e) {
            throw new RuntimeException("SignalingMessage转换为JSON失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将JSON字符串转换为SignalingMessage
     */
    public static SignalingMessage jsonStringToSignalingMessage(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        return jsonToSignalingMessage(json);
    }
}