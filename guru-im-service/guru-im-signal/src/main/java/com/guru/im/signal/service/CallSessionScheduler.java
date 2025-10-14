package com.guru.im.signal.service;

import com.guru.im.protocol.model.CallHangup;
import com.guru.im.signal.model.pojo.CallSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CallSessionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CallSessionScheduler.class);
    @Autowired
    private CallSessionService callSessionService;

    /**
     * 每分钟更新进行中通话的时长
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void updateActiveSessionDurations() {
        try {
            // 获取所有进行中的通话会话
            List<CallSession> activeSessions = callSessionService.getActiveSessions();

            for (CallSession session : activeSessions) {
                callSessionService.updateSessionDuration(session.getId());
            }

            if (!activeSessions.isEmpty()) {
                log.debug("更新了 {} 个活跃通话的时长", activeSessions.size());
            }

        } catch (Exception e) {
            log.error("更新通话时长定时任务异常", e);
        }
    }

    /**
     * 清理超时的通话会话
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void cleanupTimeoutSessions() {
        try {
            long now = System.currentTimeMillis();
            List<CallSession> dialingSessions = callSessionService.getDialingSessions();

            for (CallSession session : dialingSessions) {
                // 检查是否超时（默认30秒）
                long createTime = session.getCreateTime();
                int timeoutSeconds = session.getTimeoutSeconds() != null ? session.getTimeoutSeconds() : 30;

                if ((now - createTime) > timeoutSeconds * 1000L) {
                    log.info("通话会话超时: sessionId={}, createTime={}, timeout={}s",
                            session.getId(), createTime, timeoutSeconds);

                    callSessionService.endCallSession(
                            session.getId(),
                            "呼叫超时",
                            CallHangup.HangupType.TIMEOUT_VALUE,
                            session.getInitiatorId()
                    );
                }
            }

        } catch (Exception e) {
            log.error("清理超时会话定时任务异常", e);
        }
    }
}