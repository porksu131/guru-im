package com.guru.im.demo.signal.bridge;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.*;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户特定的JCEF管理器 - 每个用户独立进程
 */
public class UserSpecificJCEFManager {
    private static final Logger logger = LoggerFactory.getLogger(UserSpecificJCEFManager.class);
    // 添加消息路由器管理
    private final Map<Long, CefMessageRouter> windowRouters = new ConcurrentHashMap<>();
    private final AtomicLong routerCounter = new AtomicLong(0);

    private static final Map<Long, UserSpecificJCEFManager> userInstances = new ConcurrentHashMap<>();
    private final long userId;
    private CefApp cefApp;
    private CefClient cefClient;
    private boolean initialized = false;
    private final File userDataDir;

    private UserSpecificJCEFManager(long userId) {
        this.userId = userId;
        // 为每个用户创建独立的数据目录
        this.userDataDir = new File(System.getProperty("user.home"),
                ".guru-im/jcef/user-" + userId);
        this.userDataDir.mkdirs();
    }

    public static synchronized UserSpecificJCEFManager getInstance(long userId) {
        return userInstances.computeIfAbsent(userId, UserSpecificJCEFManager::new);
    }

    /**
     * 初始化用户特定的JCEF实例
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            logger.info("为用户 {} 初始化独立JCEF进程，数据目录: {}", userId, userDataDir.getAbsolutePath());

            CefAppBuilder builder = new CefAppBuilder();

            // 将jcef-bundle和jar一起打包，就不用使用时去下载了，因为下载挺慢的，130M左右，打包成exe安装后，会在app目录下。
            builder.setInstallDir(new File("app/jcef-bundle"));

            // 设置应用处理器
            builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                @Override
                public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                    logger.info("用户 {} JCEF状态变更: {}", userId, state);
                }
            });

            // 配置用户特定的设置
            CefSettings settings = builder.getCefSettings();
            settings.windowless_rendering_enabled = false;
            settings.remote_debugging_port = getDebugPortForUser(userId);
            settings.cache_path = new File(userDataDir, "cache").getAbsolutePath();
            settings.log_file = new File(userDataDir, "cef_debug.log").getAbsolutePath();

            // ========== 关键修改：完整的媒体流配置 ==========

            // 基础媒体功能
            builder.addJcefArgs("--enable-media-stream");
            builder.addJcefArgs("--enable-usermedia-screen-capturing");

            // 自动播放策略
            builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");

            // 禁用右键菜单
            builder.addJcefArgs("--disable-context-menu");

//            // 桌面捕获功能
//            builder.addJcefArgs("--enable-features=DesktopCapture");
//            builder.addJcefArgs("--enable-features=GetDisplayMedia");
//            builder.addJcefArgs("--enable-features=UserMediaScreenCapturing");
//
//            // 启用权限对话框
//            builder.addJcefArgs("--enable-permission-ui");
//
//            // 屏幕共享相关
//            builder.addJcefArgs("--allow-http-screen-capture");
//            builder.addJcefArgs("--enable-features=ScreenCapturePicker");


            // 无沙盒模式
            builder.addJcefArgs("--no-sandbox");

            // 远程调试端口，本地开发时可打开用于调试
            // builder.addJcefArgs("--remote-debugging-port=" + getDebugPortForUser(userId));
            // 用户数据目录
            builder.addJcefArgs("--user-data-dir=" + userDataDir.getAbsolutePath());

            // 构建CefApp
            cefApp = builder.build();

            cefClient = cefApp.createClient();

            cefClient.addContextMenuHandler(new CefContextMenuHandlerAdapter() {
                @Override
                public void onBeforeContextMenu(CefBrowser browser, CefFrame frame,
                                                CefContextMenuParams params, CefMenuModel model) {
                    // 清空所有默认的上下文菜单项
                    model.clear();
                }

                @Override
                public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame,
                                                    CefContextMenuParams params, int commandId, int eventFlags) {
                    // 阻止所有上下文菜单命令
                    return true;
                }

                @Override
                public void onContextMenuDismissed(CefBrowser browser, CefFrame frame) {
                    // 菜单关闭时的处理
                }
            });

            // ========== 关键：设置请求处理器 ==========
            cefClient.addRequestHandler(new CefRequestHandlerAdapter() {
                @Override
                public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request,
                                              boolean user_gesture, boolean is_redirect) {
                    return false;
                }

                @Override
                public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy,
                                                  String host, int port, String realm, String scheme,
                                                  CefAuthCallback callback) {
                    return false;
                }

            });

            // ========== 关键：设置对话框处理器 ==========
            cefClient.addDialogHandler(new CefDialogHandler() {
                @Override
                public boolean onFileDialog(CefBrowser cefBrowser, FileDialogMode fileDialogMode,
                                            String s, String s1, Vector<String> vector, Vector<String> vector1, Vector<String> vector2,
                                            CefFileDialogCallback cefFileDialogCallback) {
                    logger.info("JavaScript 对话框请求");
                    return false;
                }

            });

            cefClient.addJSDialogHandler(new CefJSDialogHandler() {
                @Override
                public boolean onJSDialog(CefBrowser cefBrowser, String s, JSDialogType jsDialogType,
                                          String messageText, String promptText,
                                          CefJSDialogCallback callback, BoolRef boolRef) {
                    // 实际运行时，这里的代码并没有执行
                    logger.info("JavaScript 对话框请求: {} - {}", jsDialogType, messageText);

                    // 自动确认所有对话框
                    if (jsDialogType == JSDialogType.JSDIALOGTYPE_ALERT ||
                            jsDialogType == JSDialogType.JSDIALOGTYPE_CONFIRM ||
                            jsDialogType == JSDialogType.JSDIALOGTYPE_PROMPT) {
                        callback.Continue(false, "");
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onBeforeUnloadDialog(CefBrowser cefBrowser, String messageText, boolean isReload,
                                                    CefJSDialogCallback callback) {
                    // 实际这里的代码并没有执行
                    logger.info("页面卸载确认: {}", messageText);
                    callback.Continue(true, "");
                    return true;
                }

                @Override
                public void onResetDialogState(CefBrowser cefBrowser) {

                }

                @Override
                public void onDialogClosed(CefBrowser cefBrowser) {

                }
            });

            initialized = true;

            logger.info("用户 {} JCEF初始化成功！",userId);

        } catch (Exception e) {
            logger.error("用户 {} JCEF初始化失败", userId, e);
            throw new RuntimeException("用户 " + userId + " JCEF初始化失败", e);
        }
    }

    /**
     * 为特定窗口创建消息路由
     */
    public CefMessageRouter createWindowRouter(long windowId) {
        // 可创建唯一的路由器名称，避免冲突
        // String queryName = "cefQuery_" + windowId + "_" + routerCounter.incrementAndGet();
        // String cancelName = "cefQueryCancel_" + windowId + "_" + routerCounter.get();
        String queryName = "cefQuery";
        String cancelName = "cefQueryCancel";

        CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig(queryName, cancelName);
        CefMessageRouter router = CefMessageRouter.create(config);

        cefClient.addMessageRouter(router);

        windowRouters.put(windowId, router);

        logger.debug("为窗口 {} 创建消息路由: {}", windowId, queryName);
        return router;
    }

    /**
     * 移除窗口的消息路由
     */
    public void removeWindowRouter(long windowId) {
        CefMessageRouter router = windowRouters.remove(windowId);
        if (router != null) {
            cefClient.removeMessageRouter(router);
            router.dispose();
            logger.debug("移除窗口 {} 的消息路由", windowId);
        }
    }

    /**
     * 根据用户ID分配调试端口
     */
    private int getDebugPortForUser(long userId) {
        return 9222 + (int) (userId % 1000); // 9222-10221范围内的端口
    }

    /**
     * 获取用户特定的CefClient
     */
    public CefClient getCefClient() {
        if (!initialized) {
            initialize();
        }
        return cefClient;
    }

    /**
     * 获取用户特定的CefApp
     */
    public CefApp getCefApp() {
        if (!initialized) {
            initialize();
        }
        return cefApp;
    }

    /**
     * 关闭用户特定的JCEF资源
     */
    public synchronized void shutdown() {
        if (cefApp != null) {
            try {
                cefApp.dispose();
                cefClient.dispose();
                initialized = false;
                userInstances.remove(userId);
                logger.info("用户 {} JCEF资源已关闭", userId);
            } catch (Exception e) {
                logger.error("关闭用户 {} JCEF资源失败", userId, e);
            }
        }
    }

    /**
     * 获取用户数据目录
     */
    public File getUserDataDir() {
        return userDataDir;
    }

    /**
     * 判断是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 关闭所有用户实例
     */
    public static void shutdownAll() {
        for (UserSpecificJCEFManager instance : userInstances.values()) {
            instance.shutdown();
        }
        userInstances.clear();
    }
}