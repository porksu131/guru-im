package com.guru.im.demo.sqlite.dao;

import com.guru.im.demo.model.FriendRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestDao {
    private final long userId;
    
    public FriendRequestDao(long userId) {
        this.userId = userId;
    }
    
    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                "user_id INTEGER NOT NULL, " +
                "id INTEGER, " +
                "requester_id INTEGER NOT NULL, " +
                "requester_name TEXT, " +
                "request_msg TEXT, " +
                "request_status INTEGER DEFAULT 0, " +
                "request_type INTEGER DEFAULT 1, " +
                "create_time INTEGER NOT NULL, " +
                "response_time INTEGER, " +
                "responder_id INTEGER, " +
                "responder_name TEXT, " +
                "PRIMARY KEY (user_id, id)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_fr_user_requester ON friend_requests(user_id, requester_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_fr_user_status ON friend_requests(user_id, request_status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_fr_user_time ON friend_requests(user_id, create_time)");
        }
    }
    
    public boolean saveFriendRequest(Connection connection, FriendRequest request) throws SQLException {
        String sql = "INSERT OR REPLACE INTO friend_requests(user_id, id, requester_id, requester_name, request_msg, " +
                "request_status, request_type, create_time, responder_id, responder_name) VALUES(?,?,?,?,?,?,?,?,?,?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, request.getId());
            pstmt.setLong(3, request.getRequesterId());
            pstmt.setString(4, request.getRequesterName());
            pstmt.setString(5, request.getRequestMsg());
            pstmt.setInt(6, request.getRequestStatus());
            pstmt.setInt(7, request.getRequestType());
            pstmt.setLong(8, request.getCreateTime());
            pstmt.setLong(9, request.getResponderId());
            pstmt.setString(10, request.getResponderName());

            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<FriendRequest> getPendingRequests(Connection connection) throws SQLException {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM friend_requests WHERE user_id = ? AND request_status = 0 ORDER BY create_time DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FriendRequest request = resultSetToFriendRequest(rs);
                requests.add(request);
            }
        }
        return requests;
    }
    
    public FriendRequest getFriendRequestById(Connection connection, Long id) throws SQLException {
        String sql = "SELECT * FROM friend_requests WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, id);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToFriendRequest(rs);
            }
        }
        return null;
    }
    
    public List<FriendRequest> getAllFriendRequests(Connection connection) throws SQLException {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM friend_requests WHERE user_id = ? ORDER BY create_time DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FriendRequest request = resultSetToFriendRequest(rs);
                requests.add(request);
            }
        }
        return requests;
    }
    
    public int getFriendRequestCount(Connection connection, long requesterId, long responseId, int requestStatus, int requestType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM friend_requests WHERE user_id = ? AND requester_id = ? AND responder_id = ? AND request_status = ? AND request_type = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, requesterId);
            pstmt.setLong(3, responseId);
            pstmt.setInt(4, requestStatus);
            pstmt.setInt(5, requestType);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public boolean updateRequestStatus(Connection connection, long id, int status, Long responseTime, Long responderId) throws SQLException {
        String sql = "UPDATE friend_requests SET request_status = ?, response_time = ?, responder_id = ? WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, status);
            pstmt.setLong(2, responseTime);
            pstmt.setLong(3, responderId);
            pstmt.setLong(4, userId);
            pstmt.setLong(5, id);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private FriendRequest resultSetToFriendRequest(ResultSet rs) throws SQLException {
        FriendRequest request = new FriendRequest();
        request.setId(rs.getLong("id"));
        request.setRequesterId(rs.getLong("requester_id"));
        request.setRequesterName(rs.getString("requester_name"));
        request.setRequestMsg(rs.getString("request_msg"));
        request.setRequestStatus(rs.getInt("request_status"));
        request.setRequestType(rs.getInt("request_type"));
        request.setCreateTime(rs.getLong("create_time"));
        request.setResponseTime(rs.getLong("response_time"));
        request.setResponderId(rs.getLong("responder_id"));
        request.setResponderName(rs.getString("responder_name"));
        return request;
    }
}