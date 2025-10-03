package com.guru.im.demo.sqlite.dao;

import com.guru.im.demo.gui.file.model.FileTransfer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileTransferDao {
    private final long userId;
    
    public FileTransferDao(long userId) {
        this.userId = userId;
    }
    
    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS file_transfers (" +
                "user_id INTEGER NOT NULL, " +
                "id TEXT, " +
                "file_name TEXT, " +
                "file_path TEXT, " +
                "file_size INTEGER, " +
                "file_type INTEGER, " +
                "content_type TEXT, " +
                "md5 TEXT, " +
                "transfer_type INTEGER NOT NULL, " +
                "status INTEGER, " +
                "progress INTEGER, " +
                "file_url TEXT, " +
                "file_id INTEGER, " +
                "client_msg_id INTEGER, " +
                "conversation_id INTEGER, " +
                "thumbnail_url TEXT, " +
                "thumbnail_path TEXT, " +
                "file_save_path TEXT, " +
                "is_pause INTEGER DEFAULT 0, " +
                "is_cancel INTEGER DEFAULT 0, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (user_id, id)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_conversation_id ON file_transfers(user_id, conversation_id);");
        }
    }
    
    public void saveFileTransfer(Connection connection, FileTransfer transfer) throws SQLException {
        String sql = "INSERT OR REPLACE INTO file_transfers (user_id, id, file_name, file_path, file_size, file_type, content_type, md5, transfer_type, status, " +
                "progress, file_url, file_id, client_msg_id, thumbnail_url,thumbnail_path, file_save_path, conversation_id, is_pause, is_cancel) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setTransferParameters(pstmt, transfer);
            pstmt.executeUpdate();
        }
    }
    
    public void updateFileTransfer(Connection connection, FileTransfer transfer) throws SQLException {
        String sql = "UPDATE file_transfers SET status = ?, progress = ?, file_url = ?, " +
                "is_pause = ?, is_cancel = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, transfer.getStatus().ordinal());
            pstmt.setInt(2, transfer.getProgress());
            pstmt.setString(3, transfer.getFileUrl());
            pstmt.setInt(4, transfer.isPaused() ? 1 : 0);
            pstmt.setInt(5, transfer.isCancelled() ? 1 : 0);
            pstmt.setLong(6, userId);
            pstmt.setString(7, transfer.getId());
            pstmt.executeUpdate();
        }
    }
    
    public List<FileTransfer> getPendingTransfersByConversation(Connection connection, Long conversationId) throws SQLException {
        List<FileTransfer> transfers = new ArrayList<>();
        String sql = "SELECT ft.* FROM file_transfers ft " +
                "INNER JOIN messages m ON ft.user_id = m.user_id AND ft.client_msg_id = m.client_msg_id " +
                "WHERE ft.user_id = ? AND ft.conversation_id = ? AND ft.status = 0 AND m.send_status = 3";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                transfers.add(resultSetToFileTransfer(rs));
            }
        }
        return transfers;
    }
    
    public FileTransfer getFileTransferByClientMsgId(Connection connection, String clientMsgId, int transferType) throws SQLException {
        String sql = "SELECT * FROM file_transfers WHERE user_id = ? AND transfer_type = ? AND client_msg_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, transferType);
            pstmt.setString(3, clientMsgId);

            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return resultSetToFileTransfer(rs);
            }
        }
        return null;
    }
    
    public List<FileTransfer> getFileTransfersByConversation(Connection connection, Long conversationId) throws SQLException {
        List<FileTransfer> transfers = new ArrayList<>();
        String sql = "SELECT * FROM file_transfers WHERE user_id = ? AND conversation_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                transfers.add(resultSetToFileTransfer(rs));
            }
        }
        return transfers;
    }
    
    public void deleteFileTransfer(Connection connection, String id) throws SQLException {
        String sql = "DELETE FROM file_transfers WHERE user_id = ? AND id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }

    public void deleteAllConversationFileTransfer(Connection connection, Long conversationId) throws SQLException {
        String sql = "DELETE FROM file_transfers WHERE user_id = ? AND conversation_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.executeUpdate();
        }
    }

    // 获取所有未完成的文件传输
    public Map<Long, List<FileTransfer>> getAllPendingTransfers(Connection connection) throws SQLException {
        Map<Long, List<FileTransfer>> transfersByConversation = new HashMap<>();

        String sql = "SELECT ft.* FROM file_transfers ft " +
                "WHERE ft.user_id = ? AND ft.status IN (0, 1, 2) " + // pending, in progress, paused
                "ORDER BY ft.conversation_id, ft.created_at DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                FileTransfer transfer = resultSetToFileTransfer(rs);
                Long conversationId = rs.getLong("conversation_id");
                if (rs.wasNull()) {
                    continue;
                }

                transfersByConversation
                        .computeIfAbsent(conversationId, k -> new ArrayList<>())
                        .add(transfer);
            }
        }

        return transfersByConversation;
    }

    // 添加遗漏的方法：通过会话ID获取文件传输
    public List<FileTransfer> getFileTransfersByConversationId(Connection connection, Long conversationId) throws SQLException {
        List<FileTransfer> transfers = new ArrayList<>();

        String sql = "SELECT * FROM file_transfers WHERE user_id = ? AND conversation_id = ? ORDER BY created_at DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transfers.add(resultSetToFileTransfer(rs));
            }
        }

        return transfers;
    }

    // 修复resultSetToFileTransfer方法，处理null值
    private FileTransfer resultSetToFileTransfer(ResultSet rs) throws SQLException {
        FileTransfer transfer = new FileTransfer();
        transfer.setId(rs.getString("id"));
        transfer.setFileName(rs.getString("file_name"));
        transfer.setFilePath(rs.getString("file_path"));
        transfer.setFileSize(rs.getLong("file_size"));
        transfer.setFileType(rs.getInt("file_type"));
        transfer.setContentType(rs.getString("content_type"));
        transfer.setMd5(rs.getString("md5"));
        transfer.setTransferType(rs.getInt("transfer_type"));
        transfer.setStatus(FileTransfer.Status.values()[rs.getInt("status")]);
        transfer.setProgress(rs.getInt("progress"));
        transfer.setFileUrl(rs.getString("file_url"));

        long fileId = rs.getLong("file_id");
        transfer.setFileId(rs.wasNull() ? null : fileId);

        transfer.setThumbnailUrl(rs.getString("thumbnail_url"));
        transfer.setThumbnailPath(rs.getString("thumbnail_path"));
        transfer.setFileSavePath(rs.getString("file_save_path"));
        transfer.setClientMsgId(rs.getString("client_msg_id"));
        transfer.setConversationId(rs.getLong("conversation_id"));

        transfer.setPaused(rs.getInt("is_pause") == 1);
        transfer.setCancelled(rs.getInt("is_cancel") == 1);

        transfer.setCreatedAt(rs.getTimestamp("created_at"));
        transfer.setUpdatedAt(rs.getTimestamp("updated_at"));

        return transfer;
    }
    
    private void setTransferParameters(PreparedStatement pstmt, FileTransfer transfer) throws SQLException {
        pstmt.setLong(1, userId);
        pstmt.setString(2, transfer.getId());
        pstmt.setString(3, transfer.getFileName());
        pstmt.setString(4, transfer.getFilePath());
        pstmt.setLong(5, transfer.getFileSize());
        pstmt.setInt(6, transfer.getFileType());
        pstmt.setString(7, transfer.getContentType());
        pstmt.setString(8, transfer.getMd5());
        pstmt.setInt(9, transfer.getTransferType());
        pstmt.setInt(10, transfer.getStatus().ordinal());
        pstmt.setInt(11, transfer.getProgress());
        pstmt.setString(12, transfer.getFileUrl());
        setNullableLong(pstmt, 13, transfer.getFileId());
        pstmt.setString(14, transfer.getClientMsgId());
        pstmt.setString(15, transfer.getThumbnailUrl());
        pstmt.setString(16, transfer.getThumbnailPath());
        pstmt.setString(17, transfer.getFileSavePath());
        pstmt.setLong( 18, transfer.getConversationId());
        pstmt.setInt(19, transfer.isPaused() ? 1 : 0);
        pstmt.setInt(20, transfer.isCancelled() ? 1 : 0);
    }
    
    private void setNullableLong(PreparedStatement pstmt, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            pstmt.setLong(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.BIGINT);
        }
    }
}