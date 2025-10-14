package com.guru.im.demo.signal.bridge;

/**
 * 浏览器消息处理器接口
 * 这个接口定义了Java端需要实现的方法，供JavaScript调用
 */
public interface BrowserMessageHandler {
    
    /**
     * 发送信令消息
     */
    void onSignalingMessage(String messageJson);
    
    /**
     * 发送普通消息
     */
    void onSendMessage(String messageJson);
    
    /**
     * 请求设备权限
     */
    void onRequestDevicePermission();
    
    /**
     * 发送Mediasoup请求
     */
    void onMediasoupRequest(String requestJson);
    
    /**
     * 关闭窗口
     */
    void onCloseWindow();
    
    /**
     * 最小化窗口
     */
    void onMinimizeWindow();
    
    /**
     * 日志记录
     */
    void onLogMessage(String level, String message);
    
    /**
     * 显示错误消息
     */
    void onShowError(String message);
    
    /**
     * 显示成功消息
     */
    void onShowSuccess(String message);
}