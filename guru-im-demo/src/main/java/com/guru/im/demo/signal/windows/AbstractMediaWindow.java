package com.guru.im.demo.signal.windows;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.DeviceInfo;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.UserService;
import com.guru.im.demo.signal.bridge.JCEFBridgeHandler;
import com.guru.im.demo.signal.bridge.UserSpecificJCEFManager;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.protocol.model.MediaType;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 媒体窗口基础抽象类 - 使用新的jcefmaven
 */
public abstract class AbstractMediaWindow extends JFrame {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractMediaWindow.class);

    protected final SignalingManager signalingManager;
    protected final MainFrame parentWindow;
    protected long sessionId;

    protected CefBrowser browser;
    protected JCEFBridgeHandler bridgeHandler;

    protected final UserInfo currentUser;
    protected final DeviceInfo deviceInfo;

    protected Object pendingInitData;
    protected String callType;
    protected MediaType mediaType;

    public AbstractMediaWindow(SignalingManager signalingManager, MediaType mediaType, MainFrame parentWindow, long sessionId) {
        this.signalingManager = signalingManager;
        this.parentWindow = parentWindow;
        this.sessionId = sessionId;
        this.currentUser = parentWindow.getCurrentUser();
        this.deviceInfo = parentWindow.getDeviceInfo();
        this.mediaType = mediaType;

        // 设置窗口属性
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(parentWindow);
    }

    /**
     * 初始化浏览器组件
     */
    protected void initializeBrowser(String htmlFile, Object initData) {
        try {
            // 存储初始化数据
            this.pendingInitData = initData;
            this.callType = getCallTypeFromHtmlFile(htmlFile);

            UserSpecificJCEFManager userJCEFManager = UserSpecificJCEFManager.getInstance(currentUser.getUid());
            CefClient cefClient = userJCEFManager.getCefClient();

            String baseUrl = getHtmlPageUrl(htmlFile, "media-page");
            String url = baseUrl + (baseUrl.contains("?") ? "&" : "?") +
                    "sessionId=" + sessionId +
                    "&userId=" + currentUser.getUid() +
                    "&callType=" + callType;

            // 创建桥接处理器
            bridgeHandler = new JCEFBridgeHandler(signalingManager, this);

            // 为每个窗口创建独立的消息路由
            CefMessageRouter messageRouter = userJCEFManager.createWindowRouter(sessionId);
            messageRouter.addHandler(bridgeHandler, true);

            // 创建浏览器实例
            browser = cefClient.createBrowser(url, false, false, null, new CefBrowserSettings());
            bridgeHandler.setBrowser(browser);

            // 创建浏览器UI组件
            Component browserUI = browser.getUIComponent();
            getContentPane().add(browserUI, BorderLayout.CENTER);

            logger.info("浏览器初始化完成: sessionId={}, url={}", sessionId, url);

        } catch (Exception e) {
            showError("初始化媒体组件失败: " + e.getMessage());
            logger.error("初始化浏览器失败", e);
        }
    }

    /**
     * 根据HTML文件名获取呼叫类型
     */
    private String getCallTypeFromHtmlFile(String htmlFile) {
        if (htmlFile.contains("audio-call")) {
            return "audio";
        } else if (htmlFile.contains("video-call")) {
            return "video";
        } else if (htmlFile.contains("screen-share")) {
            return "screen";
        } else if (htmlFile.contains("conference-call")) {
            return "conference";
        }
        return "unknown";
    }

    /**
     * 获取HTML页面URL
     */
    protected String getHtmlPageUrl(String htmlFile, String resourceFolder) {
        try {
            // 获取资源文件的绝对路径
            java.net.URL resourceUrl = getClass().getClassLoader().getResource(resourceFolder + "/" + htmlFile);
            if (resourceUrl != null) {
                String path = java.net.URLDecoder.decode(resourceUrl.getPath(), "UTF-8");
                // 确保路径格式正确
                if (path.startsWith("/") || path.contains(":")) {
                    return "file://" + path;
                } else {
                    return "file:///" + path;
                }
            }
        } catch (Exception e) {
            logger.error("获取资源路径失败", e);
        }

        // 使用相对路径
        String basePath = System.getProperty("user.dir") + "/src/main/resources/" + resourceFolder;
        return "file:///" + basePath + "/" + htmlFile;
    }

    /**
     * 添加通用窗口监听器
     */
    protected void addCommonWindowListener(Runnable closingHandler) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (closingHandler != null) {
                    closingHandler.run();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                //cleanup();
            }
        });
    }

    /**
     * 显示错误信息
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
            closeWindow();
        });
    }

    /**
     * 显示窗口
     */
    public void showWindow() {
        setVisible(true);
        toFront();
        requestFocus();
    }

    /**
     * 获取当前用户ID
     */
    public long getCurrentUserId() {
        return currentUser.getUid();
    }

    /**
     * 获取用户名称
     */
    protected String getUserName(long userId) {
        return UserService.getUserName(userId);
    }

    /**
     * 关闭窗口
     */
    public void closeWindow() {
        cleanup();
        dispose();
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 清理消息路由
        if (sessionId != 0) {
            UserSpecificJCEFManager.getInstance(currentUser.getUid()).removeWindowRouter(sessionId);
        }

        // 清理桥接处理器
        if (bridgeHandler != null) {
            bridgeHandler.dispose();
        }

        // 停止浏览器加载
        if (browser != null) {
            try {
                browser.stopLoad();
                // 注意：不要在这里关闭用户JCEF管理器，由用户会话管理
            } catch (Exception e) {
                logger.error("停止浏览器加载失败", e);
            }
        }

        logger.info("媒体窗口资源清理完成: sessionId={}", sessionId);
    }

    /**
     * 处理信令消息
     */
    public void handleSignalingMessage(SignalingMessage message) {
        if (bridgeHandler != null) {
            bridgeHandler.sendSignalingMessage(message);
        }
    }

    // Getter方法
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public CefBrowser getBrowser() {
        return browser;
    }

    public JCEFBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }

    public UserInfo getCurrentUser() {
        return currentUser;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public Object getPendingInitData() {
        return pendingInitData;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getMediaTypeName() {
        switch (mediaType) {
            case MEDIA_AUDIO_ONLY -> {
                return "语音通话";
            }
            case MEDIA_VIDEO -> {
                return "视频通话";
            }
            case MEDIA_SCREEN_SHARE -> {
                return "桌面共享";
            }
        }
        return "";
    }

}