package com.guru.im.demo.sqlite.dao;

import com.guru.im.demo.model.UserConversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserConversationDao {
    private final long userId;
    
    public UserConversationDao(long userId) {
        this.userId = userId;
    }
    
    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS user_conversation (" +
                "user_id INTEGER NOT NULL, " +
                "id INTEGER, " +
                "conversation_id INTEGER NOT NULL, " +
                "conversation_type INTEGER, " +
                "conversation_name TEXT, " +
                "conversation_key TEXT, " +
                "show_name TEXT, " +
                "avatar TEXT, " +
                "unread_count INTEGER DEFAULT 0, " +
                "is_top INTEGER DEFAULT 0, " +
                "is_mute INTEGER DEFAULT 0, " +
                "last_message_content TEXT, " +
                "last_message_time INTEGER, " +
                "last_message_sender INTEGER, " +
                "last_message_seq INTEGER, " +
                "last_sender_nickname TEXT, " +
                "create_time INTEGER, " +
                "update_time INTEGER, " +
                "last_read_seq INTEGER DEFAULT 0, " +
                "read_id INTEGER, " +
                "read_time INTEGER, " +
                "PRIMARY KEY (user_id, id)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public boolean saveOrUpdateUserConversation(Connection connection, UserConversation conversation) throws SQLException {
        String sql = "INSERT OR REPLACE INTO user_conversation VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setConversationParameters(pstmt, conversation);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<UserConversation> getUserConversations(Connection connection) throws SQLException {
        List<UserConversation> conversations = new ArrayList<>();
        String sql = "SELECT * FROM user_conversation WHERE user_id = ? ORDER BY is_top DESC, last_message_time DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                UserConversation conversation = resultSetToUserConversation(rs);
                conversations.add(conversation);
            }
        }
        return conversations;
    }
    
    public UserConversation getUserConversation(Connection connection, Long conversationId) throws SQLException {
        String sql = "SELECT * FROM user_conversation WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToUserConversation(rs);
            }
        }
        return null;
    }
    
    public boolean deleteUserConversation(Connection connection, Long conversationId) throws SQLException {
        String sql = "DELETE FROM user_conversation WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateUnreadCount(Connection connection, Long conversationId, int unreadCount) throws SQLException {
        String sql = "UPDATE user_conversation SET unread_count = ?, update_time = ? WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, unreadCount);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setLong(3, userId);
            pstmt.setLong(4, conversationId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateTopStatus(Connection connection, Long conversationId, boolean isTop) throws SQLException {
        String sql = "UPDATE user_conversation SET is_top = ?, update_time = ? WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, isTop ? 1 : 0);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setLong(3, userId);
            pstmt.setLong(4, conversationId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateMuteStatus(Connection connection, Long conversationId, boolean isMute) throws SQLException {
        String sql = "UPDATE user_conversation SET is_mute = ?, update_time = ? WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, isMute ? 1 : 0);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setLong(3, userId);
            pstmt.setLong(4, conversationId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateLastReadSeq(Connection connection, Long conversationId, Long lastReadSeq) throws SQLException {
        String sql = "UPDATE user_conversation SET last_read_seq = ?, update_time = ? WHERE user_id = ? AND conversation_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, lastReadSeq);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setLong(3, userId);
            pstmt.setLong(4, conversationId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public void batchSaveUserConversations(Connection connection, List<UserConversation> conversations) throws SQLException {
        if (conversations == null || conversations.isEmpty()) {
            return;
        }
        
        String sql = "INSERT OR REPLACE INTO user_conversation VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            
            for (UserConversation conversation : conversations) {
                setConversationParameters(pstmt, conversation);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public boolean clearUserConversations(Connection connection) throws SQLException {
        String sql = "DELETE FROM user_conversation WHERE user_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private void setConversationParameters(PreparedStatement pstmt, UserConversation conversation) throws SQLException {
        pstmt.setLong(1, conversation.getUserId());
        pstmt.setLong(2, conversation.getId());
        pstmt.setLong(3, conversation.getConversationId());
        pstmt.setInt(4, conversation.getConversationType());
        pstmt.setString(5, conversation.getConversationName());
        pstmt.setString(6, conversation.getConversationKey());
        pstmt.setString(7, conversation.getShowName());
        pstmt.setString(8, conversation.getAvatar());
        pstmt.setInt(9, conversation.getUnreadCount());
        pstmt.setInt(10, conversation.getIsTop() ? 1 : 0);
        pstmt.setInt(11, conversation.getIsMute() ? 1 : 0);
        pstmt.setString(12, conversation.getLastMessageContent());
        setNullableLong(pstmt, 13, conversation.getLastMessageTime());
        setNullableLong(pstmt, 14, conversation.getLastMessageSender());
        setNullableLong(pstmt, 15, conversation.getLastMessageSeq());
        pstmt.setString(16, conversation.getLastSenderNickname());
        setNullableLong(pstmt, 17, conversation.getCreateTime());
        setNullableLong(pstmt, 18, conversation.getUpdateTime());
        setNullableLong(pstmt, 19, conversation.getLastReadSeq());
        setNullableLong(pstmt, 20, conversation.getReadId());
        setNullableLong(pstmt, 21, conversation.getReadTime());
    }
    
    private UserConversation resultSetToUserConversation(ResultSet rs) throws SQLException {
        UserConversation conversation = new UserConversation();
        conversation.setId(rs.getLong("id"));
        conversation.setUserId(rs.getLong("user_id"));
        conversation.setConversationId(rs.getLong("conversation_id"));
        conversation.setConversationType(rs.getInt("conversation_type"));
        conversation.setConversationName(rs.getString("conversation_name"));
        conversation.setConversationKey(rs.getString("conversation_key"));
        conversation.setShowName(rs.getString("show_name"));
        conversation.setAvatar(rs.getString("avatar"));
        conversation.setUnreadCount(rs.getInt("unread_count"));
        conversation.setIsTop(rs.getInt("is_top") == 1);
        conversation.setIsMute(rs.getInt("is_mute") == 1);
        conversation.setLastMessageContent(rs.getString("last_message_content"));
        conversation.setLastMessageTime(rs.getLong("last_message_time"));
        conversation.setLastMessageSender(rs.getLong("last_message_sender"));
        conversation.setLastMessageSeq(rs.getLong("last_message_seq"));
        conversation.setLastSenderNickname(rs.getString("last_sender_nickname"));
        conversation.setCreateTime(rs.getLong("create_time"));
        conversation.setUpdateTime(rs.getLong("update_time"));
        conversation.setLastReadSeq(rs.getLong("last_read_seq"));
        conversation.setReadId(rs.getLong("read_id"));
        conversation.setReadTime(rs.getLong("read_time"));
        return conversation;
    }
    
    private void setNullableLong(PreparedStatement pstmt, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            pstmt.setLong(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.BIGINT);
        }
    }
}