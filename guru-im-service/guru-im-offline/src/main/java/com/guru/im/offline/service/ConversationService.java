package com.guru.im.offline.service;

import com.guru.im.offline.mapper.ConversationMapper;
import com.guru.im.offline.model.pojo.ConversationLatestSeq;
import com.guru.im.offline.model.pojo.ConversationUnreadCount;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversationService {
    
    private final ConversationMapper conversationMapper;
    
    public ConversationService(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }
    
    /**
     * 获取用户各会话的未读数量
     */
    public Map<Long, Integer> getUnreadCounts(Long userId) {
        List<ConversationUnreadCount> unreadCounts = conversationMapper.findUnreadCountsByUserId(userId);
        
        // 转换为Map
        Map<Long, Integer> result = new HashMap<>();
        for (ConversationUnreadCount item : unreadCounts) {
            result.put(item.getConversationId(), item.getUnreadCount());
        }
        return result;
    }
    
    /**
     * 获取用户会话列表的最新序列号
     */
    public Map<Long, Long> getLatestSeqs(Long userId) {
        List<ConversationLatestSeq> latestSeqs = conversationMapper.findLatestSeqsByUserId(userId);
        
        // 转换为Map
        Map<Long, Long> result = new HashMap<>();
        for (ConversationLatestSeq item : latestSeqs) {
            result.put(item.getConversationId(), item.getLatestSeq());
        }
        return result;
    }
}