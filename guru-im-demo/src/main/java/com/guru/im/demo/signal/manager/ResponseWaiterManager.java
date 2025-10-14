package com.guru.im.demo.signal.manager;

import com.guru.im.protocol.model.SignalingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResponseWaiterManager {
    private static final Logger logger = LoggerFactory.getLogger(ResponseWaiterManager.class);
    
    private final ConcurrentMap<Long, ResponseFuture> waitingFutures = new ConcurrentHashMap<>();
    private final long defaultTimeoutMs;
    
    public ResponseWaiterManager(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }
    
    public SignalingMessage waitForResponse(long messageId) throws InterruptedException, TimeoutException {
        return waitForResponse(messageId, defaultTimeoutMs, TimeUnit.MILLISECONDS);
    }
    
    public SignalingMessage waitForResponse(long messageId, long timeout, TimeUnit unit) 
            throws InterruptedException, TimeoutException {
        ResponseFuture future = new ResponseFuture();
        waitingFutures.put(messageId, future);
        
        try {
            boolean completed = future.latch.await(timeout, unit);
            if (!completed) {
                waitingFutures.remove(messageId);
                throw new TimeoutException("等待响应超时, messageId: " + messageId);
            }
            return future.response;
        } catch (InterruptedException e) {
            waitingFutures.remove(messageId);
            throw e;
        }
    }
    
    public void notifyResponse(SignalingMessage response) {
        Long originalMessageId = getOriginalMessageId(response);
        if (originalMessageId != 0) {
            ResponseFuture future = waitingFutures.remove(originalMessageId);
            if (future != null) {
                future.response = response;
                future.latch.countDown();
            }
        }
    }
    
    private Long getOriginalMessageId(SignalingMessage response) {
        return response.getMessageId(); // 关联的原始请求ID
    }
    
    private static class ResponseFuture {
        private final CountDownLatch latch = new CountDownLatch(1);
        private SignalingMessage response;
    }
}