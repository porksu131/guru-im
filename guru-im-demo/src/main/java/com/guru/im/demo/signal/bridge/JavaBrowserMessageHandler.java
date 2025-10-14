package com.guru.im.demo.signal.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 浏览器消息处理器的具体实现
 */
public class JavaBrowserMessageHandler implements BrowserMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(JavaBrowserMessageHandler.class);
    
    private final JCEFBridgeHandler bridgeHandler;
    
    public JavaBrowserMessageHandler(JCEFBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }
    
    @Override
    public void onSignalingMessage(String messageJson) {
    }
    
    @Override
    public void onSendMessage(String messageJson) {
    }
    
    @Override
    public void onRequestDevicePermission() {
    }
    
    @Override
    public void onMediasoupRequest(String requestJson) {
    }
    
    @Override
    public void onCloseWindow() {
        if (bridgeHandler.getCallWindow() != null) {
            bridgeHandler.getCallWindow().closeWindow();
        }
    }
    
    @Override
    public void onMinimizeWindow() {
        if (bridgeHandler.getCallWindow() != null) {
        }
    }
    
    @Override
    public void onLogMessage(String level, String message) {
        switch (level.toLowerCase()) {
            case "error":
                logger.error("[Browser] {}", message);
                break;
            case "warn":
                logger.warn("[Browser] {}", message);
                break;
            case "info":
                logger.info("[Browser] {}", message);
                break;
            case "debug":
                logger.debug("[Browser] {}", message);
                break;
            default:
                logger.trace("[Browser] {}", message);
        }
    }
    
    @Override
    public void onShowError(String message) {
        if (bridgeHandler.getCallWindow() != null) {
            bridgeHandler.getCallWindow().showError(message);
        }
    }
    
    @Override
    public void onShowSuccess(String message) {

    }
}