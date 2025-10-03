package com.guru.im.offline.service;

import com.guru.im.offline.mapper.MessageMapper;
import com.guru.im.offline.model.pojo.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    
    private final MessageMapper messageMapper;
    
    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }
    
    /**
     * 获取会话的最近消息（用于全量同步）
     */
    public List<Message> getRecentMessages(Integer conversationType, Long conversationId,
                                           Long maxSeq, int limit) {
        return messageMapper.findRecentMessages(conversationType, conversationId, maxSeq, limit);
    }
    
    /**
     * 获取会话的消息数量
     */
    public Integer countMessages(Integer conversationType, Long conversationId, Long maxSeq) {
        return messageMapper.countMessages(conversationType, conversationId, maxSeq);
    }
    
    /**
     * 获取会话的最大序列号
     */
    public Long getMaxServerSeq(Integer conversationType, Long conversationId) {
        return messageMapper.findMaxServerSeq(conversationType, conversationId);
    }
}