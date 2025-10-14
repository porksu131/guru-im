package com.guru.im.demo.signal.manager;

import com.guru.im.protocol.model.CallResponse;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 呼叫超时管理器
 */
public class CallTimeoutManager {
    private static final Logger logger = LoggerFactory.getLogger(CallTimeoutManager.class);
    
    private final SignalingManager signalingManager;
    private final MediaWindowManager windowManager;
    private final Map<Long, TimeoutTask> timeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 默认超时时间（秒）
    private static final int DEFAULT_CALL_TIMEOUT = 30;
    private static final int DEFAULT_RINGING_TIMEOUT = 45;
    
    public CallTimeoutManager(SignalingManager signalingManager, MediaWindowManager windowManager) {
        this.signalingManager = signalingManager;
        this.windowManager = windowManager;
    }

    /**
     * 启动呼叫超时检查（主叫方）
     */
    public void startCallTimeout(long sessionId, long targetUserId, boolean isVideoCall) {
        startCallTimeout(sessionId, targetUserId, isVideoCall, DEFAULT_CALL_TIMEOUT);
    }

    public void startCallTimeout(long sessionId, long targetUserId, boolean isVideoCall, int timeoutSeconds) {
        // 检查窗口是否成功创建
        if (!windowManager.hasMediaWindow(sessionId)) {
            logger.warn("媒体窗口未成功创建，不启动超时检查: sessionId={}", sessionId);
            return;
        }

        cancelTimeout(sessionId); // 取消可能存在的旧任务

        TimeoutTask task = new TimeoutTask(sessionId, targetUserId, isVideoCall, TimeoutType.OUTGOING_CALL);
        ScheduledFuture<?> future = scheduler.schedule(task, timeoutSeconds, TimeUnit.SECONDS);

        timeoutTasks.put(sessionId, new TimeoutTask(sessionId, targetUserId, isVideoCall, TimeoutType.OUTGOING_CALL, future));
        logger.info("启动呼叫超时检查: sessionId={}, timeout={}s", sessionId, timeoutSeconds);
    }
    
    /**
     * 启动来电超时检查（被叫方）
     */
    public void startIncomingCallTimeout(long sessionId, long callerId, boolean isVideoCall) {
        startIncomingCallTimeout(sessionId, callerId, isVideoCall, DEFAULT_RINGING_TIMEOUT);
    }

    public void startIncomingCallTimeout(long sessionId, long callerId, boolean isVideoCall, int timeoutSeconds) {
        cancelTimeout(sessionId);

        TimeoutTask task = new TimeoutTask(sessionId, callerId, isVideoCall, TimeoutType.INCOMING_CALL);
        ScheduledFuture<?> future = scheduler.schedule(task, timeoutSeconds, TimeUnit.SECONDS);

        timeoutTasks.put(sessionId, new TimeoutTask(sessionId, callerId, isVideoCall, TimeoutType.INCOMING_CALL, future));
        logger.info("启动来电超时检查: sessionId={}, timeout={}s", sessionId, timeoutSeconds);
    }
    
    /**
     * 取消超时检查
     */
    public void cancelTimeout(long sessionId) {
        TimeoutTask task = timeoutTasks.remove(sessionId);
        if (task != null && task.future != null) {
            task.future.cancel(false);
            logger.info("取消超时检查: sessionId={}", sessionId);
        }
    }

    /**
     * 处理设备初始化失败
     */
    public void handleDeviceInitFailed(long sessionId) {
        logger.info("设备初始化失败，取消超时检查: sessionId={}", sessionId);
        cancelTimeout(sessionId);

        // 通知用户
        windowManager.showErrorDialog(null, "设备初始化失败，请检查麦克风权限");
    }

    /**
     * 处理呼叫超时
     */
    public void handleCallTimeout(long sessionId) {
        TimeoutTask task = timeoutTasks.remove(sessionId);
        if (task != null && task.future != null) {
            task.future.cancel(false);
        }

        logger.info("处理呼叫超时: sessionId={}", sessionId);

        // 只有窗口存在时才发送超时信令
        if (windowManager.hasMediaWindow(sessionId)) {
            sendTimeoutSignaling(sessionId, task);
        }

        // 通知窗口管理器
        windowManager.handleCallTimeout(sessionId, false);

        // 清理资源
        cleanupTimeout(sessionId);
    }
    
    /**
     * 发送超时信令
     */
    private void sendTimeoutSignaling(long sessionId, TimeoutTask task) {
        try {
            SignalingMessage timeoutMessage = SignalingMessage.newBuilder()
                .setMessageId(System.currentTimeMillis())
                .setSessionId(sessionId)
                .setFromUser(getCurrentUserId())
                .setFromDevice(windowManager.getMainFrame().getDeviceInfo().getDeviceId())
                .addToUsers(task != null ? task.targetUserId : 0)
                .setType(SignalingType.CALL_TIMEOUT)
                .setTimestamp(System.currentTimeMillis())
                .setCallResponse(CallResponse.newBuilder()
                    .setAccepted(false)
                    .setReason("call timeout")
                    .build())
                .build();
                
            signalingManager.forwardToNettyAsync(timeoutMessage);
            logger.info("发送呼叫超时信令: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("发送超时信令失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 清理超时资源
     */
    private void cleanupTimeout(long sessionId) {
        timeoutTasks.remove(sessionId);
        logger.debug("清理超时资源: sessionId={}", sessionId);
    }
    
    /**
     * 获取当前用户ID
     */
    private long getCurrentUserId() {
        // 从主窗口获取当前用户ID
        return windowManager.getMainFrame().getCurrentUser().getUid();
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        timeoutTasks.clear();
        logger.info("呼叫超时管理器已关闭");
    }
    
    /**
     * 超时任务
     */
    private class TimeoutTask implements Runnable {
        final long sessionId;
        final long targetUserId;
        final boolean isVideoCall;
        final TimeoutType timeoutType;
        final ScheduledFuture<?> future;
        
        TimeoutTask(long sessionId, long targetUserId, boolean isVideoCall, TimeoutType timeoutType) {
            this(sessionId, targetUserId, isVideoCall, timeoutType, null);
        }
        
        TimeoutTask(long sessionId, long targetUserId, boolean isVideoCall, TimeoutType timeoutType, ScheduledFuture<?> future) {
            this.sessionId = sessionId;
            this.targetUserId = targetUserId;
            this.isVideoCall = isVideoCall;
            this.timeoutType = timeoutType;
            this.future = future;
        }
        
        @Override
        public void run() {
            logger.info("呼叫超时触发: sessionId={}, type={}", sessionId, timeoutType);
            handleCallTimeout(sessionId);
        }
    }
    
    /**
     * 超时类型
     */
    private enum TimeoutType {
        OUTGOING_CALL,   // 呼出通话超时
        INCOMING_CALL    // 来电超时
    }
}