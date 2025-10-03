package com.guru.im.demo.sqlite.dao;

import java.sql.*;

public class SyncStateDao {
    private final long userId;
    
    public SyncStateDao(long userId) {
        this.userId = userId;
    }
    
    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS sync_state (" +
                "user_id INTEGER NOT NULL, " +
                "last_sync_seq INTEGER DEFAULT 0, " +
                "update_time INTEGER NOT NULL, " +
                "PRIMARY KEY (user_id)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public long getLastSyncSeq(Connection connection) throws SQLException {
        String sql = "SELECT last_sync_seq FROM sync_state WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_sync_seq");
            } else {
                insertDefaultSyncState(connection);
                return 0;
            }
        }
    }
    
    public void updateLastSyncSeq(Connection connection, long seq) throws SQLException {
        String sql = "INSERT OR REPLACE INTO sync_state (user_id, last_sync_seq, update_time) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, seq);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }
    
    private void insertDefaultSyncState(Connection connection) throws SQLException {
        String sql = "INSERT INTO sync_state (user_id, last_sync_seq, update_time) VALUES (?, 0, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }
}