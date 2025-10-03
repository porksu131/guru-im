package com.guru.im.mq.starter.core;

import com.guru.im.mq.starter.core.exception.MessageProcessingException;
import com.guru.im.mq.starter.core.message.MQMessageWrapper;
import com.guru.im.mq.starter.core.router.MQMessageRouter;
import org.apache.rocketmq.spring.core.RocketMQListener;

public abstract class DefaultMessageReceiver implements RocketMQListener<MQMessageWrapper> {

    protected final MQMessageRouter messageRouter;

    public DefaultMessageReceiver(MQMessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void onMessage(MQMessageWrapper wrapper) {
        try {
            // 路由处理
            messageRouter.route(wrapper);

        } catch (Exception e) {
            // 异常处理
            handleMessageError(wrapper, e);
        }
    }

    private void handleMessageError(MQMessageWrapper wrapper, Exception e) {
        // 重试逻辑
        if (shouldRetry(wrapper, e)) {
            throw new RuntimeException("触发重试", e);
        }
        // 死信队列处理
        // sendToDLQ(mqMsg, e);
    }

    private boolean shouldRetry(MQMessageWrapper wrapper, Exception e) {
        if (e instanceof MessageProcessingException) {
            return ((MessageProcessingException) e).isShouldRetry();
        }
        return false;
    }
}