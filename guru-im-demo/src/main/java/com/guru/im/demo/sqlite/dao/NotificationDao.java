package com.guru.im.demo.sqlite.dao;

import com.guru.im.demo.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {
    private final long userId;
    
    public NotificationDao(long userId) {
        this.userId = userId;
    }
    
    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS notifications (" +
                "user_id INTEGER NOT NULL, " +
                "id INTEGER, " +
                "type INTEGER, " +
                "correlation_id INTEGER, " +
                "title TEXT, " +
                "content TEXT, " +
                "timestamp INTEGER, " +
                "is_read INTEGER DEFAULT 0, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (user_id, correlation_id)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            // 创建索引以提高查询性能
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(user_id, type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(user_id, is_read);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_timestamp ON notifications(user_id, timestamp DESC);");
        }
    }
    
    public void saveNotification(Connection connection, Notification notification) throws SQLException {
        String sql = "INSERT OR REPLACE INTO notifications (user_id, type, correlation_id, title, content, timestamp, is_read, id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNotificationParameters(pstmt, notification);
            pstmt.executeUpdate();
            
            // 获取自增ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                notification.setId(generatedKeys.getLong(1));
            }
        }
    }
    
    public void updateNotification(Connection connection, Notification notification) throws SQLException {
        String sql = "UPDATE notifications SET type = ?, correlation_id = ?, title = ?, content = ?, " +
                "timestamp = ?, is_read = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, notification.getType());
            setNullableLong(pstmt, 2, notification.getCorrelationId());
            pstmt.setString(3, notification.getTitle());
            pstmt.setString(4, notification.getContent());
            pstmt.setLong(5, notification.getTimestamp());
            pstmt.setInt(6, notification.getRead() ? 1 : 0);
            pstmt.setLong(7, userId);
            pstmt.setLong(8, notification.getId());
            pstmt.executeUpdate();
        }
    }
    
    public void markAsRead(Connection connection, Long notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, notificationId);
            pstmt.executeUpdate();
        }
    }
    
    public void markAllAsRead(Connection connection) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND is_read = 0";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    public Notification getNotificationById(Connection connection, int id) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, id);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToNotification(rs);
            }
        }
        return null;
    }
    
    public List<Notification> getNotificationsByType(Connection connection, Integer type) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND type = ? ORDER BY timestamp DESC, id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, type);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(resultSetToNotification(rs));
            }
        }
        return notifications;
    }
    
    public List<Notification> getUnreadNotifications(Connection connection) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = 0 ORDER BY timestamp DESC, id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(resultSetToNotification(rs));
            }
        }
        return notifications;
    }
    
    public List<Notification> getAllNotifications(Connection connection) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC, id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(resultSetToNotification(rs));
            }
        }
        return notifications;
    }
    
    public List<Notification> getRecentNotifications(Connection connection, int limit) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC, id DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(resultSetToNotification(rs));
            }
        }
        return notifications;
    }
    
    public int getUnreadCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public void deleteNotification(Connection connection, int id) throws SQLException {
        String sql = "DELETE FROM notifications WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }
    
    public void deleteNotificationsByType(Connection connection, Integer type) throws SQLException {
        String sql = "DELETE FROM notifications WHERE user_id = ? AND type = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, type);
            pstmt.executeUpdate();
        }
    }
    
    public void deleteAllNotifications(Connection connection) throws SQLException {
        String sql = "DELETE FROM notifications WHERE user_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    // 清理过期的通知（例如30天前的）
    public void cleanupExpiredNotifications(Connection connection, long expireTimestamp) throws SQLException {
        String sql = "DELETE FROM notifications WHERE user_id = ? AND timestamp < ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, expireTimestamp);
            pstmt.executeUpdate();
        }
    }
    
    private Notification resultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setType(rs.getInt("type"));
        
        long correlationId = rs.getLong("correlation_id");
        notification.setCorrelationId(rs.wasNull() ? null : correlationId);
        
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setTimestamp(rs.getLong("timestamp"));
        notification.setRead(rs.getInt("is_read") == 1);
        
        return notification;
    }
    
    private void setNotificationParameters(PreparedStatement pstmt, Notification notification) throws SQLException {
        pstmt.setLong(1, userId);
        pstmt.setInt(2, notification.getType());
        setNullableLong(pstmt, 3, notification.getCorrelationId());
        pstmt.setString(4, notification.getTitle());
        pstmt.setString(5, notification.getContent());
        pstmt.setLong(6, notification.getTimestamp());
        pstmt.setInt(7, notification.getRead() ? 1 : 0);
        pstmt.setLong(8, notification.getId());
    }
    
    private void setNullableLong(PreparedStatement pstmt, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            pstmt.setLong(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.BIGINT);
        }
    }
}