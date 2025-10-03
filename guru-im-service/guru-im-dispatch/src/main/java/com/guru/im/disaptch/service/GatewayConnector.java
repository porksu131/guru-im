package com.guru.im.disaptch.service;

import com.guru.im.common.model.DeviceStatus;
import com.guru.im.protocol.model.ImMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 网关连接器抽象接口
 * 负责获取用户连接信息和向用户推送消息
 */
public interface GatewayConnector {
    /**
     * 获取用户所有设备状态信息
     *
     * @param uid 用户ID
     * @return 所有设备网关路由信息列表
     */
    List<DeviceStatus> getUserAllGatewayNodes(Long uid);

    /**
     * 获取用户当前连接的所有设备所在的网关节点
     *
     * @param uid 用户ID
     * @return 所有设备网关路由信息列表
     */
    List<DeviceStatus> getUserDeviceStatus(Long uid, int onlineStatus);

    /**
     * 获取用户设备连接的所在的网关节点
     *
     * @param uid      用户ID
     * @param deviceId 设备ID
     * @return 设备网关路由信息
     */
    DeviceStatus getUserDeviceGatewayNode(Long uid, String deviceId);

    /**
     * 异步推送消息到指定的网关节点
     *
     * @param gatewayRoute 目标网关节点
     * @param imMessage    要推送的消息信封
     * @return CompletableFuture 用于异步处理结果
     */
    CompletableFuture<ImMessage> pushMessageToGateway(Long uid, DeviceStatus gatewayRoute, ImMessage imMessage);

    /**
     * 异步推送消息到多个网关节点
     *
     * @param gatewayRoutes 目标网关节点列表
     * @param imMessage     要推送的消息信封
     * @return CompletableFuture 用于异步处理结果列表
     */
    CompletableFuture<List<ImMessage>> pushMessageToGateways(Long uid, List<DeviceStatus> gatewayRoutes, ImMessage imMessage);


    void pushMessageByOneway(Long uid, DeviceStatus gatewayRoute, ImMessage imMessage);

    void pushMessageByOneway(Long uid, List<DeviceStatus> gatewayRoutes, ImMessage imMessage);
}