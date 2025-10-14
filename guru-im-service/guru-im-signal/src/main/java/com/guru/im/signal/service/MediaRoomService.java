package com.guru.im.signal.service;


import com.guru.im.signal.model.pojo.MediaRoomMapping;

import java.util.Map;

public interface MediaRoomService {
    
    /**
     * 创建媒体房间
     */
    MediaRoomMapping createMediaRoom(Long sessionId, String peerId);

    /**
     * 加入媒体房间
     */
    Map<String, Object> joinMediaRoom(Long sessionId, String peerId);

    /**
     * 获取房间信息
     */
    MediaRoomMapping getMediaRoomBySessionId(Long sessionId);
    
    /**
     * 获取房间RTP能力
     */
    Map<String, Object> getRoomRtpCapabilities(String roomId);
    
    /**
     * 创建WebRTC传输
     */
    Map<String, Object> createWebRtcTransport(String roomId, String peerId);
    
    /**
     * 连接传输
     */
    void connectTransport(String roomId, String transportId, Map<String, Object> dtlsParameters);
    
    /**
     * 创建生产者
     */
    Map<String, Object> produce(String roomId, String transportId, String kind, Map<String, Object> rtpParameters, String peerId);
    
    /**
     * 创建消费者
     */
    Map<String, Object> consume(String roomId, String transportId, String producerId, Map<String, Object> rtpCapabilities, String peerId);
    
    /**
     * 恢复消费者
     */
    void resumeConsumer(String roomId, String consumerId, String peerId);
    
    /**
     * 获取房间内的生产者列表
     */
    Map<String, Object> getRoomProducers(String roomId);
    
    /**
     * 关闭媒体房间
     */
    void closeMediaRoom(Long sessionId);
    
    /**
     * 清理媒体房间
     */
    void cleanupMediaRoom(Long sessionId);
    
    /**
     * 检查房间是否存在
     */
    boolean isRoomActive(Long sessionId);
}