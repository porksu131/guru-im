package com.guru.im.cache.starter.distribute.id;

import com.guru.im.cache.starter.CacheConstant;
import com.guru.im.common.exception.ServiceException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

// 严格有序的ID生成器
public class SequenceIdGenerator {
    private final RedisTemplate<String, Long> redisTemplate;

    public SequenceIdGenerator(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 生成会话内严格有序ID（带重试机制）
    public Long nextConversationSeq(Long conversationId) {
        String redisKey = CacheConstant.CONVERSATION_SEQ + "{" + conversationId + "}"; // {}保证集群同节点

        // 重试3次（应对Redis短暂抖动）
        for (int i = 0; i < 3; i++) {
            try {
                return redisTemplate.opsForValue().increment(redisKey, 1L);
            } catch (RedisConnectionFailureException e) {
                try {
                    Thread.sleep(10 * (i + 1)); // 指数退避
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        throw new ServiceException("ID生成失败，请降级处理");
    }

    // 生成全局有序ID
    public Long nextGlobalSeq() {
        String redisKey = CacheConstant.IM_GLOBAL_SEQ;
        try {
            return redisTemplate.opsForValue().increment(redisKey, 1L);
        } catch (RedisConnectionFailureException e) {
            throw new RuntimeException(e);
        }
    }
}