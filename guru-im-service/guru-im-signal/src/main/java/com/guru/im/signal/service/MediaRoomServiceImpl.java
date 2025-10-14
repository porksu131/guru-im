package com.guru.im.signal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.signal.mapper.MediaRoomMapper;
import com.guru.im.signal.model.pojo.MediaRoomMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MediaRoomServiceImpl implements MediaRoomService {

    private static final Logger log = LoggerFactory.getLogger(MediaRoomServiceImpl.class);
    @Value("${mediasoup.http.host:localhost}")
    private String mediaServerHost;

    @Value("${mediasoup.http.port:3000}")
    private int mediaServerPort;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MediaRoomMapper mediaRoomMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private static final String MEDIA_SERVER_BASE_URL = "http://%s:%d";

    @Override
    public MediaRoomMapping createMediaRoom(Long sessionId, String peerId) {
        String roomId = "room_" + sessionId;
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            // 调用mediasoup API创建房间
            String url = baseUrl + "/room/join";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("peerId", peerId);

            log.info("创建媒体房间请求: url={}, request={}", url, request);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    MediaRoomMapping mediaRoomMapping = mediaRoomMapper.selectBySessionId(sessionId);
                    if (mediaRoomMapping == null) {
                        // 保存房间映射关系
                        MediaRoomMapping mediaRoom = new MediaRoomMapping();
                        mediaRoom.setId(snowflakeIdGenerator.nextId());
                        mediaRoom.setSessionId(sessionId);
                        mediaRoom.setRoomId(roomId);
                        mediaRoom.setRoomStatus(1); // 活跃状态
                        mediaRoom.setActivePeers(1); // 初始有一个peer（服务器）
                        mediaRoom.setCreateTime(System.currentTimeMillis());
                        mediaRoom.setUpdateTime(System.currentTimeMillis());

                        mediaRoomMapper.insert(mediaRoom);

                        log.info("创建媒体房间成功: sessionId={}, roomId={}", sessionId, roomId);

                        return mediaRoom;
                    }

                    return mediaRoomMapping;
                }
            }

            log.error("创建媒体房间失败: sessionId={}, response={}", sessionId, response);
            throw new RuntimeException("加入媒体房间失败");

        } catch (Exception e) {
            log.error("创建媒体房间异常: sessionId={}", sessionId, e);
            throw new RuntimeException("创建媒体房间异常: " + e.getMessage());
        }
    }

    public Map<String, Object> joinMediaRoom(Long sessionId, String peerId) {
        String roomId = "room_" + sessionId;
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            // 调用mediasoup API创建房间
            String url = baseUrl + "/room/join";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("peerId", peerId);

            log.info("加入媒体房间请求: url={}, request={}", url, request);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {
                    });
                }
            }

            log.error("加入媒体房间失败: sessionId={}, response={}", sessionId, response);
            throw new RuntimeException("加入媒体房间失败");

        } catch (Exception e) {
            log.error("加入媒体房间异常: sessionId={}", sessionId, e);
            throw new RuntimeException("加入媒体房间异常: " + e.getMessage());
        }
    }

    @Override
    public MediaRoomMapping getMediaRoomBySessionId(Long sessionId) {
        return mediaRoomMapper.selectBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> getRoomRtpCapabilities(String roomId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            // 使用新的专门接口查询RTP能力
            String url = baseUrl + "/room/rtp-capabilities";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {
                    });
                }
            }

            log.error("获取房间RTP能力失败: roomId={}", roomId);
            throw new RuntimeException("获取房间RTP能力失败");

        } catch (Exception e) {
            log.error("获取房间RTP能力异常: roomId={}", roomId, e);
            throw new RuntimeException("获取房间RTP能力异常: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> createWebRtcTransport(String roomId, String peerId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/transport/create";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("peerId", peerId);

            log.info("创建WebRTC传输请求: url={}, request={}", url, request);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    Map<String, Object> transport = (Map<String, Object>) data.get("transport");

                    // 更新活跃peer数量
                    updateActivePeers(roomId, 1);

                    log.info("创建WebRTC传输成功: roomId={}, peerId={}, transportId={}",
                            roomId, peerId, transport.get("id"));
                    return transport;
                }
            }

            log.error("创建WebRTC传输失败: roomId={}, peerId={}", roomId, peerId);
            throw new RuntimeException("创建WebRTC传输失败");

        } catch (Exception e) {
            log.error("创建WebRTC传输异常: roomId={}, peerId={}", roomId, peerId, e);
            throw new RuntimeException("创建WebRTC传输异常: " + e.getMessage());
        }
    }

    @Override
    public void connectTransport(String roomId, String transportId, Map<String, Object> dtlsParameters) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/transport/connect";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("transportId", transportId);
            request.put("dtlsParameters", dtlsParameters);
            request.put("peerId", "connect_" + System.currentTimeMillis());

            log.info("连接传输请求: url={}, transportId={}", url, transportId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    log.info("连接传输成功: roomId={}, transportId={}", roomId, transportId);
                    return;
                }
            }

            log.error("连接传输失败: roomId={}, transportId={}", roomId, transportId);
            throw new RuntimeException("连接传输失败");

        } catch (Exception e) {
            log.error("连接传输异常: roomId={}, transportId={}", roomId, transportId, e);
            throw new RuntimeException("连接传输异常: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> produce(String roomId, String transportId, String kind,
                                       Map<String, Object> rtpParameters, String peerId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/produce";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("transportId", transportId);
            request.put("kind", kind);
            request.put("rtpParameters", rtpParameters);
            request.put("peerId", peerId);

            log.info("创建生产者请求: url={}, kind={}, peerId={}", url, kind, peerId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    log.info("创建生产者成功: roomId={}, peerId={}, kind={}, producerId={}",
                            roomId, peerId, kind, data.get("id"));
                    return data;
                }
            }

            log.error("创建生产者失败: roomId={}, peerId={}, kind={}", roomId, peerId, kind);
            throw new RuntimeException("创建生产者失败");

        } catch (Exception e) {
            log.error("创建生产者异常: roomId={}, peerId={}, kind={}", roomId, peerId, kind, e);
            throw new RuntimeException("创建生产者异常: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> consume(String roomId, String transportId, String producerId,
                                       Map<String, Object> rtpCapabilities, String peerId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/consume";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("transportId", transportId);
            request.put("producerId", producerId);
            request.put("rtpCapabilities", rtpCapabilities);
            request.put("peerId", peerId);

            log.info("创建消费者请求: url={}, producerId={}, peerId={}", url, producerId, peerId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    log.info("创建消费者成功: roomId={}, peerId={}, producerId={}, consumerId={}",
                            roomId, peerId, producerId, data.get("id"));
                    return data;
                }
            }

            log.error("创建消费者失败: roomId={}, peerId={}, producerId={}", roomId, peerId, producerId);
            throw new RuntimeException("创建消费者失败");

        } catch (Exception e) {
            log.error("创建消费者异常: roomId={}, peerId={}, producerId={}", roomId, peerId, producerId, e);
            throw new RuntimeException("创建消费者异常: " + e.getMessage());
        }
    }

    @Override
    public void resumeConsumer(String roomId, String consumerId, String peerId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/consumer/resume";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("consumerId", consumerId);
            request.put("peerId", peerId);

            log.info("恢复消费者请求: url={}, consumerId={}, peerId={}", url, consumerId, peerId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    log.info("恢复消费者成功: roomId={}, consumerId={}, peerId={}", roomId, consumerId, peerId);
                    return;
                }
            }

            log.error("恢复消费者失败: roomId={}, consumerId={}, peerId={}", roomId, consumerId, peerId);
            throw new RuntimeException("恢复消费者失败");

        } catch (Exception e) {
            log.error("恢复消费者异常: roomId={}, consumerId={}, peerId={}", roomId, consumerId, peerId, e);
            throw new RuntimeException("恢复消费者异常: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRoomProducers(String roomId) {
        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);

        try {
            String url = baseUrl + "/room/producers";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);

            log.info("获取房间生产者列表请求: url={}, roomId={}", url, roomId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("msg"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    log.info("获取房间生产者列表成功: roomId={}, producersCount={}",
                            roomId, ((List<Map<String, Object>>) data.get("producers")).size());
                    return data;
                }
            }

            log.error("获取房间生产者列表失败: roomId={}", roomId);
            throw new RuntimeException("获取房间生产者列表失败");

        } catch (Exception e) {
            log.error("获取房间生产者列表异常: roomId={}", roomId, e);
            throw new RuntimeException("获取房间生产者列表异常: " + e.getMessage());
        }
    }

    @Override
    public void closeMediaRoom(Long sessionId) {
        MediaRoomMapping mediaRoom = mediaRoomMapper.selectBySessionId(sessionId);
        if (mediaRoom == null) {
            log.warn("媒体房间不存在: sessionId={}", sessionId);
            return;
        }

        String baseUrl = String.format(MEDIA_SERVER_BASE_URL, mediaServerHost, mediaServerPort);
        String roomId = mediaRoom.getRoomId();

        try {
            // 调用离开房间接口
            String url = baseUrl + "/room/leave";

            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("peerId", "server_" + sessionId);

            log.info("关闭媒体房间请求: url={}, roomId={}", url, roomId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // 更新房间状态为关闭
                mediaRoomMapper.updateRoomStatus(roomId, 0, System.currentTimeMillis());
                log.info("关闭媒体房间成功: sessionId={}, roomId={}", sessionId, roomId);
            } else {
                log.error("关闭媒体房间失败: sessionId={}, roomId={}, response={}", sessionId, roomId, response);
            }

        } catch (Exception e) {
            log.error("关闭媒体房间异常: sessionId={}, roomId={}", sessionId, roomId, e);
        }
    }

    @Override
    public void cleanupMediaRoom(Long sessionId) {
        MediaRoomMapping mediaRoom = mediaRoomMapper.selectBySessionId(sessionId);
        if (mediaRoom == null) {
            return;
        }

        // 关闭媒体房间
        closeMediaRoom(sessionId);

        // 删除映射记录
        mediaRoomMapper.deleteBySessionId(sessionId);

        log.info("清理媒体房间完成: sessionId={}, roomId={}", sessionId, mediaRoom.getRoomId());
    }

    @Override
    public boolean isRoomActive(Long sessionId) {
        MediaRoomMapping mediaRoom = mediaRoomMapper.selectBySessionId(sessionId);
        return mediaRoom != null && mediaRoom.getRoomStatus() == 1;
    }

    /**
     * 更新活跃peer数量
     */
    private void updateActivePeers(String roomId, int delta) {
        try {
            MediaRoomMapping mediaRoom = mediaRoomMapper.selectByRoomId(roomId);
            if (mediaRoom != null) {
                int newActivePeers = Math.max(0, mediaRoom.getActivePeers() + delta);
                mediaRoomMapper.updateActivePeers(roomId, newActivePeers, System.currentTimeMillis());
                log.debug("更新活跃peer数量: roomId={}, activePeers={}", roomId, newActivePeers);
            }
        } catch (Exception e) {
            log.error("更新活跃peer数量异常: roomId={}", roomId, e);
        }
    }
}