package com.guru.im.demo.signal.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.guru.im.demo.signal.converter.MessageConverter;
import com.guru.im.demo.signal.manager.SignalingManager;
import com.guru.im.demo.signal.model.MediaDataClasses;
import com.guru.im.demo.signal.windows.AbstractMediaWindow;
import com.guru.im.protocol.model.*;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 处理浏览器与Java主程序之间的通信
 */
public class JCEFBridgeHandler extends CefMessageRouterHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(JCEFBridgeHandler.class);
    private static final Gson gson = new Gson();

    private final BrowserMessageHandler messageHandler;
    private final SignalingManager signalingManager;
    private final AbstractMediaWindow callWindow;
    private CefBrowser browser;
    private static final Map<Long, JCEFBridgeHandler> sessionHandlers = new ConcurrentHashMap<>();
    private final long sessionId;

    private final ExecutorService executorService;

    public JCEFBridgeHandler(SignalingManager signalingManager, AbstractMediaWindow callWindow) {
        this.signalingManager = signalingManager;
        this.messageHandler = new JavaBrowserMessageHandler(this);
        this.callWindow = callWindow;
        this.sessionId = callWindow.getSessionId();
        this.executorService = Executors.newCachedThreadPool();
        sessionHandlers.put(sessionId, this);         // 注册会话处理器
    }

    public void setBrowser(CefBrowser browser) {
        this.browser = browser;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                           String request, boolean persistent, CefQueryCallback callback) {
        try {
            logger.debug("收到浏览器请求: {}", request);
            JsonObject jsonRequest = JsonParser.parseString(request).getAsJsonObject();

            // 尝试从请求中提取sessionId
            Long targetSessionId = extractSessionId(jsonRequest);
            if (targetSessionId != null && targetSessionId != this.sessionId) {
                // 转发到正确的处理器
                JCEFBridgeHandler targetHandler = sessionHandlers.get(targetSessionId);
                if (targetHandler != null) {
                    return targetHandler.handleDirectQuery(jsonRequest, callback);
                }
            }

            // 当前处理器处理
            return handleDirectQuery(jsonRequest, callback);

        } catch (Exception e) {
            logger.error("路由请求失败", e);
            callback.failure(-1, "路由失败: " + e.getMessage());
            return true;
        }
    }

    private Long extractSessionId(JsonObject request) {
        try {
            // 直接最外层取
            if (request.has("sessionId")) {
                return request.get("sessionId").getAsLong();
            }

            // 从data中提取
            if (request.has("data")) {
                JsonObject data = request.get("data").getAsJsonObject();
                if (data.has("sessionId")) {
                    return data.get("sessionId").getAsLong();
                }
            }


        } catch (Exception e) {
            logger.warn("提取sessionId失败", e);
        }

        return null;
    }

    public boolean handleDirectQuery(JsonObject jsonRequest, CefQueryCallback callback) {
        try {
            String method = jsonRequest.get("method").getAsString();
            JsonObject data = jsonRequest.has("data") ? jsonRequest.get("data").getAsJsonObject() : new JsonObject();
            String requestId = jsonRequest.has("requestId") ? jsonRequest.get("requestId").getAsString() : null;

            // 异步处理请求
            CompletableFuture<JsonObject> future = handleBrowserRequest(method, data, requestId);

            future.thenAccept(response -> {
                callback.success(gson.toJson(response));
            }).exceptionally(error -> {
                logger.error("处理请求失败: {}", method, error);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("error", error.getMessage());
                if (requestId != null) {
                    errorResponse.addProperty("requestId", requestId);
                }
                callback.success(gson.toJson(errorResponse));
                return null;
            });

            return true;
        } catch (Exception e) {
            logger.error("处理浏览器请求失败", e);
            callback.failure(-1, e.getMessage());
            return true;
        }
    }

    // 处理浏览器发来请求
    private CompletableFuture<JsonObject> handleBrowserRequest(String method, JsonObject data, String requestId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject response = new JsonObject();
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }

                switch (method) {
                    case "requestCallInitData":
                        String callInitDataJson = getCallInitData();
                        response.addProperty("data", callInitDataJson);
                        response.addProperty("success", true);
                        break;

                    case "devicesReady":
                        handleDevicesReady(data, response);
                        response.addProperty("success", true);
                        break;

                    case "devicesFailed":
                        handleDevicesFailed(data);
                        response.addProperty("success", true);
                        break;

                    case "joinRoom":
                        handleJoinRoom(data, response);
                        response.addProperty("success", true);
                        break;

                    case "callAccepted":
                        handleCallAccepted(data, response);
                        response.addProperty("success", true);
                        break;

                    case "callRejected":
                        handleCallRejected(data, response);
                        response.addProperty("success", true);
                        break;

                    case "callCancelled":
                        handleCallCancelled(data, response);
                        response.addProperty("success", true);
                        break;

                    case "conferenceCancelled":
                        handleConferenceCancelled(data);
                        response.addProperty("success", true);
                        break;

                    case "callEnded":
                        handleCallEnded(data, response);
                        response.addProperty("success", true);
                        break;

                    case "callTimeout":
                        handleCallTimeout(data, response);
                        response.addProperty("success", true);
                        break;

                    case "getRouterRtpCapabilities":
                        handleGetRouterRtpCapabilities(data, response);
                        response.addProperty("success", true);
                        break;

                    case "createTransport":
                        handleCreateTransport(data, response);
                        response.addProperty("success", true);
                        break;

                    case "connectTransport":
                        handleConnectTransport(data, response);
                        response.addProperty("success", true);
                        break;

                    case "produce":
                        handleProduce(data, response);
                        response.addProperty("success", true);
                        break;

                    case "consume":
                        handleConsume(data, response);
                        response.addProperty("success", true);
                        break;

                    case "resumeConsumer":
                        handleResumeConsumer(data, response);
                        response.addProperty("success", true);
                        break;

                    case "getRoomProducers":
                        handleGetRoomProducers(data, response);
                        response.addProperty("success", true);
                        break;

                    case "sendSignaling":
                        messageHandler.onSignalingMessage(data.toString());
                        handleSendSignalingMessage(data, response);
                        response.addProperty("success", true);
                        break;

                    case "sendMessage":
                        messageHandler.onSendMessage(data.toString());
                        handleSendMessage(data);
                        response.addProperty("success", true);
                        break;

                    case "requestDevicePermission":
                        messageHandler.onRequestDevicePermission();
                        JsonObject permissionResult = handleDevicePermission();
                        response.add("data", permissionResult);
                        response.addProperty("success", true);
                        break;

                    case "closeWindow":
                        messageHandler.onCloseWindow();
                        callWindow.closeWindow();
                        response.addProperty("success", true);
                        break;

                    case "minimizeWindow":
                        messageHandler.onMinimizeWindow();
                        //callWindow.setExtendedState(java.awt.Frame.ICONIFIED);
                        response.addProperty("success", true);
                        break;

                    case "log":
                        String level = data.get("level").getAsString();
                        String message = data.get("message").getAsString();
                        //messageHandler.onLogMessage(level, message);
                        //handleLogMessage(data);
                        response.addProperty("success", true);
                        break;

                    case "showError":
                        messageHandler.onShowError(data.get("message").getAsString());
                        response.addProperty("success", true);
                        break;

                    case "showSuccess":
                        messageHandler.onShowSuccess(data.get("message").getAsString());
                        response.addProperty("success", true);
                        break;

                    default:
                        throw new IllegalArgumentException("未知的方法: " + method);
                }

                return response;
            } catch (Exception e) {
                logger.error("处理请求 {} 失败", method, e);
                JsonObject errorResponse = new JsonObject();
                if (requestId != null) {
                    errorResponse.addProperty("requestId", requestId);
                }
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("error", e.getMessage());
                return errorResponse;
            }
        }, executorService);
    }


    private String getCallInitData() {
        String dataJson = gson.toJson(callWindow.getPendingInitData());
        logger.debug("InitData: {}", dataJson);
        return dataJson;
    }

    /**
     * 发送超时信令
     */
    private void handleCallTimeout(JsonObject data, JsonObject response) {

        try {
            long sessionId = data.get("sessionId").getAsLong();
            long targetUserId = data.get("targetUserId").getAsLong();

            SignalingMessage timeoutMessage = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(getDeviceId())
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_TIMEOUT)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setReason("call timeout")
                            .build())
                    .build();
            logger.info("发送呼叫超时信令: sessionId={}", sessionId);

            forwardToNetty(timeoutMessage, response);

        } catch (Exception e) {
            logger.error("发送呼叫超时信令失败", e);
            response.addProperty("success", false);
        }
    }

    /**
     * 加入房间
     */
    private void handleJoinRoom(JsonObject data, JsonObject response) {
        try {
            long sessionId = data.get("sessionId").getAsLong();

            SignalingMessage callRequest = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(getDeviceId())
                    .addToUsers(getCurrentUserId())
                    .setType(SignalingType.JOIN_ROOM)
                    .build();
            logger.info("发送加入房间请求: sessionId={}", sessionId);

            forwardToNetty(callRequest, response);

        } catch (Exception e) {
            logger.error("发送加入房间请求失败", e);
            response.addProperty("success", false);
        }

    }

    /**
     * 处理设备就绪通知
     */
    private void handleDevicesReady(JsonObject data, JsonObject response) {
        try {
            long sessionId = data.get("sessionId").getAsLong();
            long targetUserId = data.get("targetUserId").getAsLong();
            String deviceId = data.get("deviceId").getAsString();

            logger.info("浏览器设备就绪: sessionId={}, targetUserId={}, MediaType={}",
                    sessionId, targetUserId, callWindow.getMediaType());

            SignalingMessage callRequest = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(deviceId)
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_REQUEST)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallRequest(CallRequest.newBuilder()
                            .setMediaType(callWindow.getMediaType())
                            .addTargetUsers(targetUserId)
                            .setTimeoutSeconds(30)
                            .build())
                    .build();

            logger.info("发送呼叫请求: sessionId={}, targetUserId={}, mediaType={}",
                    sessionId, targetUserId, callWindow.getMediaType().toString());

            forwardToNetty(callRequest, response);

            // 通知MediaWindowManager
            signalingManager.getMediaWindowManager().onDevicesReady(sessionId, targetUserId, callWindow.getMediaType());

        } catch (Exception e) {
            logger.error("发送呼叫请求失败", e);
            response.addProperty("success", false);
        }
    }

    /**
     * 处理设备初始化失败
     */
    private void handleDevicesFailed(JsonObject data) {
        try {
            long sessionId = data.get("sessionId").getAsLong();
            String error = data.get("error").getAsString();

            logger.warn("浏览器设备初始化失败: sessionId={}, error={}", sessionId, error);

            // 通知MediaWindowManager
            signalingManager.getMediaWindowManager().onDevicesFailed(sessionId, error);

        } catch (Exception e) {
            logger.error("处理设备失败通知失败", e);
        }
    }

    private void handleCallAccepted(JsonObject data, JsonObject response) {
        try {
            long sessionId = data.get("sessionId").getAsLong();
            long targetUserId = data.get("targetUserId").getAsLong();

            logger.info("浏览器接受通话: sessionId={}, ", sessionId);

            // 发送接受信令
            SignalingMessage acceptMessage = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(getDeviceId())
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_ACCEPT)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(true)
                            .setSelectedDevice(getDeviceId())
                            .build())
                    .build();

            forwardToNetty(acceptMessage, response);

            signalingManager.getMediaWindowManager().handleCallAccepted(sessionId, true);

        } catch (Exception e) {
            logger.error("处理通话取消通知失败", e);
        }
    }

    private void handleCallRejected(JsonObject data, JsonObject response) {
        try {
            long sessionId = data.get("sessionId").getAsLong();
            long targetUserId = data.get("targetUserId").getAsLong();

            logger.info("浏览器拒绝通话: sessionId={}, ", sessionId);

            // 发送信令
            SignalingMessage signalingMessage = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(getDeviceId())
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_REJECT)
                    .setTimestamp(System.currentTimeMillis())
                    .setCallResponse(CallResponse.newBuilder()
                            .setAccepted(false)
                            .setSelectedDevice(getDeviceId())
                            .build())
                    .build();

            forwardToNetty(signalingMessage, response);

            signalingManager.getMediaWindowManager().handleCallRejected(sessionId, true);

        } catch (Exception e) {
            logger.error("处理通话取消通知失败", e);
        }
    }

    private void handleCallCancelled(JsonObject data, JsonObject response) {
        try {
            long sessionId = data.get("sessionId").getAsLong();
            long targetUserId = data.get("targetUserId").getAsLong();

            // 发送信令
            SignalingMessage signalingMessage = SignalingMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setFromUser(getCurrentUserId())
                    .setFromDevice(getDeviceId())
                    .addToUsers(targetUserId)
                    .setType(SignalingType.CALL_CANCEL)
                    .build();

            forwardToNetty(signalingMessage, response);


            logger.info("浏览器取消通话: sessionId={}", sessionId);

            // 从活动通话中移除
            signalingManager.getMediaWindowManager().getActiveCalls().remove(sessionId);

        } catch (Exception e) {
            logger.error("处理通话取消通知失败", e);
        }
    }

    private void handleConferenceCancelled(JsonObject data) {
        String reason = data.get("reason").getAsString();
        logger.info("浏览器取消会议: reason={}", reason);

        // 关闭会议窗口
        if (callWindow != null) {
            callWindow.closeWindow();
        }
    }

    // 处理通话结束
    private void handleCallEnded(JsonObject data, JsonObject response) {
        long sessionId = data.get("sessionId").getAsLong();
        long targetUserId = data.get("targetUserId").getAsLong();
        String reason = data.get("reason").getAsString();
        logger.info("浏览器通话结束: reason={}", reason);

        // 发送信令
        SignalingMessage signalingMessage = SignalingMessage.newBuilder()
                .setSessionId(sessionId)
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .addToUsers(targetUserId)
                .setType(SignalingType.CALL_HANGUP)
                .build();

        forwardToNetty(signalingMessage, response);

        signalingManager.getMediaWindowManager().handleCallHangup(sessionId, true);
        // 清理资源
        if (callWindow != null) {
            callWindow.closeWindow();
        }
    }

    // 添加获取callWindow的方法
    public AbstractMediaWindow getCallWindow() {
        return callWindow;
    }

    private void handleSendSignalingMessage(JsonObject data, JsonObject response) {
        try {
            JsonObject messageObj = data.getAsJsonObject("message");
            SignalingMessage signalingMessage = MessageConverter.jsonToSignalingMessage(messageObj);

            // 通过信令管理器发送
            forwardToNetty(signalingMessage, response);
            logger.info("转发信令消息到Netty: {}", signalingMessage.getType());

        } catch (Exception e) {
            logger.error("处理信令消息失败", e);
            throw new RuntimeException("信令消息处理失败: " + e.getMessage(), e);
        }
    }

    private void handleSendMessage(JsonObject data) {
        // 处理普通消息发送
        String messageType = data.get("type").getAsString();
        JsonObject messageData = data.getAsJsonObject("data");
        logger.debug("处理消息发送: {}", messageType);
    }

    private JsonObject handleDevicePermission() {
        JsonObject result = new JsonObject();
        result.addProperty("audioGranted", true);
        result.addProperty("videoGranted", true);
        result.addProperty("message", "设备权限已授权");
        return result;
    }

    private void handleLogMessage(JsonObject data) {
        String level = data.get("level").getAsString();
        String message = data.get("message").getAsString();

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
            default:
                logger.debug("[Browser] {}", message);
        }
    }

    // Mediasoup相关方法实现
    private void handleGetRouterRtpCapabilities(JsonObject data, JsonObject response) {
        String roomId = data.get("roomId").getAsString();

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.RTP_CAPABILITIES_REQUEST)
                .setRtpCapabilitiesRequest(RtpCapabilitiesRequest.newBuilder()
                        .setRoomId(roomId)
                        .build())
                .build();

        forwardToNetty(request, response);

        logger.debug("发送RTP能力请求: roomId={}", roomId);
    }

    private void handleCreateTransport(JsonObject data, JsonObject response) {
        String direction = data.get("direction").getAsString();
        String roomId = data.get("roomId").getAsString();

        // 将传输选项转换为JSON字符串
        JsonObject transportOptions = data.getAsJsonObject("options");
        String transportOptionsJson = gson.toJson(transportOptions);

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.MEDIASOUP_TRANSPORT_CREATE)
                .setTransportCreate(MediasoupTransportCreate.newBuilder()
                        .setRoomId(roomId)
                        .setTransportOptions(transportOptionsJson)  // 改为使用JSON字符串
                        .build())
                .build();

        logger.debug("发送传输创建请求: direction={}, roomId={}", direction, roomId);

        forwardToNetty(request, response);
    }

    private void handleConnectTransport(JsonObject data, JsonObject response) {
        String transportId = data.get("transportId").getAsString();
        JsonObject dtlsParameters = data.getAsJsonObject("dtlsParameters");
        String dtlsParametersJson = gson.toJson(dtlsParameters);

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.MEDIASOUP_TRANSPORT_CONNECT)
                .setTransportConnect(MediasoupTransportConnect.newBuilder()
                        .setTransportId(transportId)
                        .setDtlsParameters(dtlsParametersJson)
                        .build())
                .build();

        logger.debug("发送传输连接请求: transportId={}", transportId);

        forwardToNetty(request, response);
    }

    private void handleProduce(JsonObject data, JsonObject response) {
        String transportId = data.get("transportId").getAsString();
        String kind = data.get("kind").getAsString();
        JsonObject rtpParameters = data.getAsJsonObject("rtpParameters");
        String rtpParametersJson = gson.toJson(rtpParameters);

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.MEDIASOUP_PRODUCE)
                .setProduce(MediasoupProduce.newBuilder()
                        .setTransportId(transportId)
                        .setKind(kind)
                        .setRtpParameters(rtpParametersJson)
                        .build())
                .build();

        logger.debug("发送生产者创建请求: transportId={}, kind={}", transportId, kind);
        forwardToNetty(request, response);
    }

    private void handleConsume(JsonObject data, JsonObject response) {
        String transportId = data.get("transportId").getAsString();
        String producerId = data.get("producerId").getAsString();
        JsonObject rtpCapabilities = data.getAsJsonObject("rtpCapabilities");
        String rtpCapabilitiesJson = gson.toJson(rtpCapabilities);

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.MEDIASOUP_CONSUME)
                .setConsume(MediasoupConsume.newBuilder()
                        .setTransportId(transportId)
                        .setProducerId(producerId)
                        .setRtpCapabilities(rtpCapabilitiesJson)
                        .build())
                .build();

        logger.debug("发送消费者创建请求: transportId={}, producerId={}", transportId, producerId);
        forwardToNetty(request, response);
    }

    private void handleResumeConsumer(JsonObject data, JsonObject response) {
        String consumerId = data.get("consumerId").getAsString();

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.MEDIASOUP_CONSUMER_RESUME)
                .setConsumerResume(MediasoupConsumerResume.newBuilder()
                        .setConsumerId(consumerId)
                        .build())
                .build();

        logger.debug("发送消费者恢复请求: consumerId={}", consumerId);

        forwardToNetty(request, response);
    }

    private void handleGetRoomProducers(JsonObject data, JsonObject response) {
        String roomId = data.get("roomId").getAsString();

        SignalingMessage request = SignalingMessage.newBuilder()
                .setSessionId(getSessionId())
                .setFromUser(getCurrentUserId())
                .setFromDevice(getDeviceId())
                .setType(SignalingType.ROOM_PRODUCERS_REQUEST)
                .setRoomProducersRequest(RoomProducersRequest.newBuilder()
                        .setRoomId(roomId)
                        .build())
                .build();

        logger.debug("发送房间生产者请求: roomId={}", roomId);

        forwardToNetty(request, response);
    }

    private long getSessionId() {
        return callWindow.getSessionId();
    }

    private long getCurrentUserId() {
        return callWindow.getCurrentUserId();
    }

    private String getDeviceId() {
        return callWindow.getDeviceInfo().getDeviceId();
    }

    /**
     * 发送通话初始化数据到浏览器
     */
    public void sendCallInitData(MediaDataClasses.CallInitData initData) {
        if (browser != null && !browser.isLoading()) {
            try {
                String dataJson = gson.toJson(initData);
                String script = String.format("window.handleCallInitData && window.handleCallInitData(%s)", dataJson);
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话初始化数据到浏览器: sessionId={}", initData.sessionId);
            } catch (Exception e) {
                logger.error("发送通话初始化数据失败", e);
                // 重试机制
                SwingUtilities.invokeLater(() -> {
                    try {
                        String dataJson = gson.toJson(initData);
                        String script = String.format("setTimeout(() => window.handleCallInitData && window.handleCallInitData(%s), 1000)", dataJson);
                        browser.executeJavaScript(script, browser.getURL(), 0);
                    } catch (Exception ex) {
                        logger.error("重试发送通话初始化数据失败", ex);
                    }
                });
            }
        } else {
            // 浏览器还在加载，延迟发送
            SwingUtilities.invokeLater(() -> {
                sendCallInitData(initData);
            });
        }
    }

    /**
     * 发送通话接受消息到浏览器
     */
    public void sendCallAccepted() {
        if (browser != null) {
            try {
                String script = "window.handleCallAccepted()";
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话接受消息到浏览器");
            } catch (Exception e) {
                logger.error("发送通话接受消息失败", e);
            }
        }
    }

    /**
     * 发送通话拒绝消息到浏览器
     */
    public void sendCallRejected(String reason) {
        if (browser != null) {
            try {
                String script = String.format("window.handleCallRejected('%s')", reason);
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话拒绝消息到浏览器: {}", reason);
            } catch (Exception e) {
                logger.error("发送通话拒绝消息失败", e);
            }
        }
    }

    /**
     * 发送通话挂断消息到浏览器
     */
    public void sendCallHangup() {
        if (browser != null) {
            try {
                String script = "window.handleCallHangup()";
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话挂断消息到浏览器");
            } catch (Exception e) {
                logger.error("发送通话挂断消息失败", e);
            }
        }
    }


    public void sendCallTimeOut() {
        if (browser != null) {
            try {
                String script = "window.handleCallTimeout()";
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话超时消息到浏览器");
            } catch (Exception e) {
                logger.error("发送通话超时消息失败", e);
            }
        }
    }

    /**
     * 发送通话取消消息到浏览器
     */
    public void sendCallCancel() {
        if (browser != null) {
            try {
                String script = "window.handleCallCancel()";
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送通话取消消息到浏览器");
            } catch (Exception e) {
                logger.error("发送通话取消消息失败", e);
            }
        }
    }

    /**
     * 发送信令消息到浏览器
     */
    public void sendSignalingMessage(SignalingMessage message) {
        if (browser != null) {
            try {
                String messageJson = MessageConverter.signalingMessageToJson(message);
                String script = String.format("window.handleSignalingMessage(%s)", messageJson);
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送信令消息到浏览器: {}", message.getType());
            } catch (Exception e) {
                logger.error("发送信令消息到浏览器失败", e);
            }
        }
    }

    /**
     * 发送会议初始化数据到浏览器
     */
    public void sendConferenceInitData(MediaDataClasses.ConferenceInitData initData) {
        if (browser != null && !browser.isLoading()) {
            try {
                String dataJson = gson.toJson(initData);
                String script = String.format("window.handleConferenceInitData && window.handleConferenceInitData(%s)", dataJson);
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送会议初始化数据到浏览器: conferenceId={}", initData.sessionId);
            } catch (Exception e) {
                logger.error("发送会议初始化数据失败", e);
                // 重试机制
                SwingUtilities.invokeLater(() -> {
                    try {
                        String dataJson = gson.toJson(initData);
                        String script = String.format("setTimeout(() => window.handleConferenceInitData && window.handleConferenceInitData(%s), 1000)", dataJson);
                        browser.executeJavaScript(script, browser.getURL(), 0);
                    } catch (Exception ex) {
                        logger.error("重试发送会议初始化数据失败", ex);
                    }
                });
            }
        } else {
            // 浏览器还在加载，延迟发送
            SwingUtilities.invokeLater(() -> {
                sendConferenceInitData(initData);
            });
        }
    }

    /**
     * 发送参与者加入消息到浏览器
     */
    public void sendParticipantJoined(long userId, String userName) {
        if (browser != null) {
            try {
                String script = String.format("window.handleParticipantJoined(%d, '%s')", userId, userName);
                browser.executeJavaScript(script, browser.getURL(), 0);
            } catch (Exception e) {
                logger.error("发送参与者加入消息失败", e);
            }
        }
    }

    /**
     * 发送参与者离开消息到浏览器
     */
    public void sendParticipantLeft(long userId) {
        if (browser != null) {
            try {
                String script = String.format("window.handleParticipantLeft(%d)", userId);
                browser.executeJavaScript(script, browser.getURL(), 0);
            } catch (Exception e) {
                logger.error("发送参与者离开消息失败", e);
            }
        }
    }

    /**
     * 发送错误消息到浏览器
     */
    public void sendErrorMessage(String message) {
        if (browser != null) {
            try {
                String script = String.format("window.handleError('%s')", message);
                browser.executeJavaScript(script, browser.getURL(), 0);
                logger.debug("发送错误消息到浏览器: {}", message);
            } catch (Exception e) {
                logger.error("发送错误消息到浏览器失败", e);
            }
        }
    }

    /**
     * 清理资源
     */
    public void dispose() {
        executorService.shutdown();
    }

    public void forwardToNetty(SignalingMessage message, JsonObject response) {
        SignalingType type = message.getType();
        if (requiresResponse(type)) {
            try {
                SignalingMessage signalingMessage = signalingManager.forwardToNetty(
                        message, getTimeoutForType(type), TimeUnit.MILLISECONDS
                );
                response.addProperty("data", JsonFormat.printer().print(signalingMessage));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        } else {
            signalingManager.forwardToNettyAsync(message);
        }
    }

    private boolean requiresResponse(SignalingType type) {
        switch (type) {
            // 通话控制
            case CALL_REQUEST:
            case JOIN_ROOM:
//            case CALL_ACCEPT:
//            case CALL_REJECT:

                // 媒体传输
            case MEDIASOUP_TRANSPORT_CREATE:
            case MEDIASOUP_TRANSPORT_CONNECT:
            case MEDIASOUP_PRODUCE:
            case MEDIASOUP_CONSUME:
            case MEDIASOUP_CONSUMER_RESUME:

                // 信息查询
            case RTP_CAPABILITIES_REQUEST:
            case ROOM_PRODUCERS_REQUEST:

                // 会议控制
            case CONFERENCE_INVITE:
//            case CONFERENCE_JOIN:
//            case CONFERENCE_LEAVE:
//            case CONFERENCE_REJOIN:
//            case CONFERENCE_REJECT:
                return true;

            default:
                return false;
        }
    }

    /**
     * 获取不同类型信令的超时时间
     */
    private long getTimeoutForType(SignalingType type) {
        switch (type) {
            case CALL_REQUEST:
                return 30000; // 呼叫请求30秒超时

            case MEDIASOUP_TRANSPORT_CREATE:
            case MEDIASOUP_TRANSPORT_CONNECT:
            case MEDIASOUP_PRODUCE:
            case MEDIASOUP_CONSUME:
                return 15000; // 媒体操作15秒超时

            case RTP_CAPABILITIES_REQUEST:
            case ROOM_PRODUCERS_REQUEST:
                return 30000; // 查询请求10秒超时

            case CONFERENCE_INVITE:
            case CONFERENCE_JOIN:
                return 20000; // 会议操作20秒超时

            default:
                return 30000; // 默认30秒
        }
    }

}