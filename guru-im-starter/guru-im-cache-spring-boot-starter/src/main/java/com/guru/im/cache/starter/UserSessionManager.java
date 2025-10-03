package com.guru.im.cache.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.model.DeviceStatus;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UserSessionManager {

    private static final Logger log = LoggerFactory.getLogger(UserSessionManager.class);
    private final RedisTemplate<String, Map<String, Object>> mapRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public UserSessionManager(RedisTemplate<String, Map<String, Object>> mapRedisTemplate,
                              RedisTemplate<String, Object> redisTemplate) {
        this.mapRedisTemplate = mapRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    private static final Map<String, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    public void addLocalSession(String address, Channel value) {
        CHANNEL_CACHE.put(address, value);
    }

    public Channel getLocalSession(String address) {
        return CHANNEL_CACHE.get(address);
    }

    public void removeLocalSession(String address) {
        CHANNEL_CACHE.remove(address);
    }

    public void saveLocalSession(String address, Channel channel) {
        addLocalSession(address, channel);
    }

    public void addRedisSession(String key, Map<String, Object> value) {
        mapRedisTemplate.opsForValue().set(key, value, 7, TimeUnit.DAYS);
    }

    public void removeRedisSession(String key) {
        mapRedisTemplate.delete(key);
    }

    public Map<String, Object> getRedisSession(String key) {
        return mapRedisTemplate.opsForValue().get(key);
    }

//    public Map<String, Object> getUserRedisSession(Long userId) {
//        return mapRedisTemplate.opsForValue().get(CacheConstant.USER_GATEWAY_SERVER + userId);
//    }
//
//    public void addUserRedisSession(Long userId, Map<String, Object> value) {
//        mapRedisTemplate.opsForValue().set(CacheConstant.USER_GATEWAY_SERVER + userId, value, 7, TimeUnit.DAYS);
//    }
//
//    public void removeUserRedisSession(Long userId) {
//        mapRedisTemplate.delete(CacheConstant.USER_GATEWAY_SERVER + userId);
//    }

//    // 设置用户在线状态
//    public void setUserOnline(Long userId) {
//        redisTemplate.opsForValue().set(CacheConstant.USER_ONLINE_STATUS + userId, OnlineStatus.ONLINE, 90, TimeUnit.SECONDS);
//        redisTemplate.opsForValue().set(CacheConstant.USER_ACTIVE_TIME + userId, System.currentTimeMillis());
//    }
//
//    // 设置用户离线状态
//    public void setUserOffline(Long userId) {
//        redisTemplate.opsForValue().set(CacheConstant.USER_ONLINE_STATUS + userId, OnlineStatus.OFFLINE);
//    }

    /**
     * 获取指定用户指定设备的在线状态和路由信息
     */
    public DeviceStatus getDeviceRoute(Long userId, String deviceId) {
        String redisKey = getUserDeviceRedisKey(userId);
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        // HGET user_device_status:10086 device_iphone13
        String routeInfoJson = hashOps.get(redisKey, deviceId);

        if (routeInfoJson == null) {
            log.debug("Device not found online. UserId: {}, DeviceId: {}", userId, deviceId);
            return null;
        }

        try {
            return objectMapper.readValue(routeInfoJson, DeviceStatus.class);
        } catch (IOException e) {
            log.error("Failed to deserialize JSON to route info to for user: {}, device: {}", userId, deviceId, e);
            return null;
        }
    }

    /**
     * 获取指定用户所有在线设备的路由信息
     */
    public Map<String, String> getAllDeviceStatus(Long userId) {
        String redisKey = getUserDeviceRedisKey(userId);
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        // HGETALL user_device_status:10086
        Map<String, String> allDevices = hashOps.entries(redisKey);

        log.debug("Found {} online devices for user: {}", allDevices.size(), userId);
        return allDevices;
    }

    /**
     * 获取指定用户所有在线设备的路由信息
     */
    public List<DeviceStatus> getAllDeviceRouteInfo(Long userId) {
        Map<String, String> allDevices = getAllDeviceStatus(userId);
        if (allDevices.isEmpty()) {
            log.debug("User is not online on any device: {}", userId);
            return null;
        }

        List<DeviceStatus> routeInfos = new ArrayList<>();
        for (Map.Entry<String, String> entry : allDevices.entrySet()) {
            String deviceId = entry.getKey();
            String routeInfoJson = entry.getValue();

            DeviceStatus deviceStatus = null;
            try {
                deviceStatus = objectMapper.readValue(routeInfoJson, DeviceStatus.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            routeInfos.add(deviceStatus);
        }
        return routeInfos;
    }


    /**
     * 更新设备状态（网关层调用）
     *
     * @param userId         用户ID
     * @param deviceId       设备ID
     * @param gatewayAddress 网关节点地址
     * @param clientVersion  设备客户端版本号
     * @param platform       设备平台
     */
    public void updateDeviceStatus(Long userId, String deviceId, String gatewayAddress,
                                   String clientVersion, String platform,
                                   Integer onlineStatus) {
        String redisKey = getUserDeviceRedisKey(userId);
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        // 构造Value的JSON内容
        DeviceStatus routeInfo = new DeviceStatus(deviceId, gatewayAddress, clientVersion,
                System.currentTimeMillis(), platform, onlineStatus);
        String routeInfoJson;
        try {
            routeInfoJson = objectMapper.writeValueAsString(routeInfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize route info to JSON for user: {}, device: {}", userId, deviceId, e);
            return;
        }

        // HSET user_device_status:10086 device_iphone13 {json}
        hashOps.put(redisKey, deviceId, routeInfoJson);

        // 为整个Key设置TTL（例如7天），自动清理长时间离线的用户数据
        redisTemplate.expire(redisKey, Duration.ofDays(7));

        log.info("Device status updated. User: {}, Device: {}, onlineStatus:{}, Node: {}",
                userId, deviceId, onlineStatus, gatewayAddress);
    }

    /**
     * 设备下线，移除状态（网关层调用）
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    public void removeDeviceStatus(Long userId, String deviceId) {
        String redisKey = getUserDeviceRedisKey(userId);
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        // HDEL user_device_status:10086 device_iphone13
        Long removed = hashOps.delete(redisKey, deviceId);

        if (removed > 0) {
            log.info("Device status removed. User: {}, Device: {}", userId, deviceId);
        }
    }

    /**
     * 判断用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        List<DeviceStatus> allDeviceStatus = getAllDeviceRouteInfo(userId);
        if (allDeviceStatus != null && !allDeviceStatus.isEmpty()) {
            return allDeviceStatus.stream().anyMatch(obj -> obj.getOnlineStatus() == OnlineStatus.ONLINE);
        }
        return false;
    }

    /**
     * 批量获取用户的在线设备
     * @param userIds 用户ID列表
     * @return Map<用户ID, 该用户的在线设备列表>
     */
    public Map<Long, List<DeviceStatus>> batchGetUsersOnlineDevices(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量构造Redis key
        List<String> redisKeys = userIds.stream()
                .map(this::getUserDeviceRedisKey)
                .toList();

        // 使用pipeline批量获取所有用户的设备信息
        List<Object> pipelineResults = redisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    @Override
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        for (String key : redisKeys) {
                            connection.hashCommands().hGetAll(key.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    }
                }
        );

        Map<Long, List<DeviceStatus>> result = new HashMap<>(userIds.size());

        // 处理每个用户的设备信息
        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            Object pipelineResult = pipelineResults.get(i);

            if (!(pipelineResult instanceof Map)) {
                result.put(userId, Collections.emptyList());
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> deviceMapBytes = (Map<String, String>) pipelineResult;

            if (deviceMapBytes.isEmpty()) {
                result.put(userId, Collections.emptyList());
                continue;
            }

            List<DeviceStatus> onlineDevices = new ArrayList<>();

            // 解析每个设备的JSON数据并筛选在线设备
            for (Map.Entry<String, String> entry : deviceMapBytes.entrySet()) {
                try {
                    String deviceId = entry.getKey();
                    String deviceStatusJson = entry.getValue();

                    DeviceStatus deviceStatus = objectMapper.readValue(deviceStatusJson, DeviceStatus.class);
                    if (deviceStatus.getOnlineStatus() == OnlineStatus.ONLINE) {
                        onlineDevices.add(deviceStatus);
                    }
                } catch (IOException e) {
                    log.error("Failed to deserialize device status for user: {}", userId, e);
                }
            }

            result.put(userId, onlineDevices);
        }

        return result;
    }

    /**
     * 获取在线或离线的设备信息
     */
    public List<DeviceStatus> getUserDeviceStatus(Long userId, int onlineStatus) {
        List<DeviceStatus> allDeviceStatus = getAllDeviceRouteInfo(userId);
        if (allDeviceStatus != null && !allDeviceStatus.isEmpty()) {
            return allDeviceStatus.stream()
                    .filter(obj -> obj.getOnlineStatus() == onlineStatus)
                    .toList();
        }
        return null;
    }

    private String getUserDeviceRedisKey(Long userId) {
        return CacheConstant.USER_DEVICE_GATEWAY_SERVER + userId;
    }
}
