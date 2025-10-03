package com.guru.im.demo.sqlite.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.constant.MessageType;
import com.guru.im.demo.gui.file.model.FileTransfer;
import com.guru.im.demo.model.MediaInfo;
import com.guru.im.demo.model.Message;

import java.sql.*;
import java.util.*;

public class MessageDao {
    private final long userId;
    private final FileTransferDao fileTransferManager;

    public MessageDao(long userId) {
        this.userId = userId;
        this.fileTransferManager = new FileTransferDao(userId);
    }

    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                "user_id INTEGER NOT NULL, " +
                "server_seq INTEGER, " +
                "message_id INTEGER, " +
                "message_content TEXT NOT NULL, " +
                "receiver_id INTEGER NOT NULL, " +
                "receiver_name TEXT, " +
                "sender_id INTEGER NOT NULL, " +
                "sender_name TEXT, " +
                "message_type INTEGER NOT NULL, " +
                "client_send_time INTEGER NOT NULL, " +
                "read_status INTEGER NOT NULL, " +
                "send_status INTEGER NOT NULL," +
                "client_msg_id TEXT NOT NULL," +
                "client_seq INTEGER NOT NULL," +
                "conversation_type INTEGER," +
                "conversation_id INTEGER," +
                "is_local_temp INTEGER NOT NULL," +
                "retry_count INTEGER NOT NULL, " +
                "PRIMARY KEY (user_id, client_msg_id)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_message_user_to_from ON messages (user_id, receiver_id, sender_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_seq ON messages(user_id, server_seq);");
        }
    }

    public void saveMessage(Connection connection, Message message) throws SQLException {
        String sql = "INSERT OR REPLACE INTO messages VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, message.getServerSeq() == null ? -1 : message.getServerSeq());
            pstmt.setLong(3, message.getMessageId() == null ? -1 : message.getMessageId());
            pstmt.setString(4, message.getMessageContent());
            pstmt.setLong(5, message.getReceiverId());
            pstmt.setString(6, message.getReceiverName());
            pstmt.setLong(7, message.getSenderId());
            pstmt.setString(8, message.getSenderName());
            pstmt.setInt(9, message.getMessageType());
            pstmt.setLong(10, message.getClientSendTime());
            pstmt.setInt(11, message.getReadStatus());
            pstmt.setInt(12, message.getSendStatus());
            pstmt.setString(13, message.getClientMsgId());
            pstmt.setLong(14, message.getClientSeq() == null ? -1L : message.getClientSeq());
            pstmt.setLong(15, message.getConversationType());
            pstmt.setLong(16, message.getConversationId());
            pstmt.setBoolean(17, message.isLocalTemp());
            pstmt.setInt(18, message.getRetryCount());

            pstmt.executeUpdate();
        }
    }

    /**
     * 查询指定会话中最大序列号对应的消息
     *
     * @param connection 数据库连接
     * @param conversationId 会话ID
     * @return Message 该会话中最大序列号对应的消息，如果不存在返回null
     * @throws SQLException 数据库操作异常
     */
    public Message getMaxSequenceMessageByConversation(Connection connection, long conversationId) throws SQLException {
        String sql = "SELECT m1.* FROM messages m1 " +
                "INNER JOIN (" +
                "    SELECT MAX(server_seq) as max_server_seq " +
                "    FROM messages WHERE user_id = ? AND conversation_id = ? AND server_seq > 0" +
                ") m2 ON m1.server_seq = m2.max_server_seq " +
                "WHERE m1.user_id = ? AND m1.conversation_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.setLong(3, userId);
            pstmt.setLong(4, conversationId);

            List<Message> messages = executeMessageQuery(pstmt);
            if (!messages.isEmpty()) {
                Message message = messages.get(0);
                deserializeMessage(connection, Collections.singletonList(message));
                return message;
            }
            return null;
        }
    }

    /**
     * 查询指定会话中当前用户的未读消息数量
     *
     * @param connection 数据库连接
     * @param conversationId 会话ID
     * @throws SQLException 数据库操作异常
     */
    public int getUnreadCountSentByMe(Connection connection, long conversationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE user_id = ? AND conversation_id = ? AND sender_id != ? AND read_status = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.setLong(3, userId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public long getMaxSequenceId(Connection connection, Long sessionId) throws SQLException {
        String sql = "SELECT MAX(server_seq) as server_seq FROM messages WHERE user_id = ? AND conversation_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, sessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("server_seq");
            }
        }
        return 0;
    }

    public long getMaxClientSeq(Connection connection) throws SQLException {
        String sql = "SELECT MAX(client_seq) FROM messages WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    public int updateMessagesToReadBySeq(Connection connection, long conversationId, long readSeq) throws SQLException {
        String sql = "UPDATE messages SET read_status = 2 WHERE user_id = ? AND conversation_id = ? AND server_seq <= ? AND read_status = 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.setLong(3, readSeq);
            return pstmt.executeUpdate();
        }
    }

    public int getUnreadCount(Connection connection, long peerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE user_id = ? AND receiver_id = ? AND read_status = '2'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public void batchUpdateStatus(Connection connection, List<Long> messageIds, int newStatus) throws SQLException {
        if (messageIds.isEmpty()) return;

        StringBuilder sql = new StringBuilder("UPDATE messages SET read_status = ? WHERE user_id = ? AND messageId IN (");
        for (int i = 0; i < messageIds.size(); i++) {
            sql.append("?");
            if (i < messageIds.size() - 1) sql.append(",");
        }
        sql.append(")");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setInt(1, newStatus);
            ps.setLong(2, userId);
            for (int i = 0; i < messageIds.size(); i++) {
                ps.setLong(i + 3, messageIds.get(i));
            }
            ps.executeUpdate();
        }
    }

    public void deleteMessage(Connection connection, Message message) throws SQLException {
        String sql = "DELETE FROM messages WHERE user_id = ? AND client_msg_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, message.getClientMsgId());
            pstmt.executeUpdate();
        }
    }
    public void deleteAllConversationMessage(Connection connection, Long conversationId) throws SQLException {
        String sql = "DELETE FROM messages WHERE user_id = ? AND conversation_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.executeUpdate();
        }
    }

    private List<Message> executeMessageQuery(PreparedStatement pstmt) throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Message msg = new Message();
                msg.setServerSeq(rs.getLong("server_seq"));
                msg.setMessageId(rs.getLong("message_id"));
                msg.setMessageContent(rs.getString("message_content"));
                msg.setReceiverId(rs.getLong("receiver_id"));
                msg.setReceiverName(rs.getString("receiver_name"));
                msg.setSenderId(rs.getLong("sender_id"));
                msg.setSenderName(rs.getString("sender_name"));
                msg.setMessageType(rs.getInt("message_type"));
                msg.setClientSendTime(rs.getLong("client_send_time"));
                msg.setReadStatus(rs.getInt("read_status"));
                msg.setSendStatus(rs.getInt("send_status"));
                msg.setClientMsgId(rs.getString("client_msg_id"));
                msg.setClientSeq(rs.getLong("client_seq"));
                msg.setConversationType(rs.getInt("conversation_type"));
                msg.setConversationId(rs.getLong("conversation_id"));
                msg.setLocalTemp(rs.getBoolean("is_local_temp"));
                messages.add(msg);
            }
        }
        return messages;
    }

    public List<Message> loadSendFailMessages(Connection connection, Long conversationId) throws SQLException {
        String sql = "SELECT * FROM messages WHERE user_id = ? AND conversation_id = ? AND send_status = 3 ";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            List<Message> messages = executeMessageQuery(pstmt);
            deserializeMessage(connection, messages); // 添加反序列化
            return messages;
        }
    }

    public List<Message> loadMessages(Connection connection, Long conversationId, long afterSequenceId, int limit) throws SQLException {
        String sql = "SELECT * FROM messages WHERE user_id = ? AND conversation_id = ? AND server_seq < ? ORDER BY server_seq DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, conversationId);
            pstmt.setLong(3, afterSequenceId);
            pstmt.setInt(4, limit);

            List<Message> messages = executeMessageQuery(pstmt);
            Collections.reverse(messages);
            deserializeMessage(connection, messages); // 添加反序列化
            return messages;
        }
    }

    // 添加反序列化方法
    private void deserializeMessage(Connection connection, List<Message> messages) throws SQLException {
        for (Message message : messages) {
            if (isMediaType(message.getMessageType())) {
                try {
                    MediaInfo mediaInfo = new ObjectMapper().readValue(message.getMessageContent(), MediaInfo.class);
                    message.setMediaInfo(mediaInfo);

                    // 如果是当前用户发送的消息，关联文件传输信息
                    if (message.getSenderId() == userId) {
                        FileTransfer fileTransfer = fileTransferManager.getFileTransferByClientMsgId(connection, message.getClientMsgId(), 0);
                        message.setUploadTransfer(fileTransfer);
                    }
                    FileTransfer downloadTransfer = fileTransferManager.getFileTransferByClientMsgId(connection, message.getClientMsgId(), 1);
                    message.setDownloadTransfer(downloadTransfer);

                } catch (JsonProcessingException e) {
                    throw new RuntimeException("反序列化媒体消息失败", e);
                }
            }
        }
    }

    private boolean isMediaType(int messageType) {
        return messageType == MessageType.IMAGE || messageType == MessageType.VOICE
                || messageType == MessageType.VIDEO || messageType == MessageType.FILE;
    }

    /**
     * 根据用户查询每个会话 conversation_id 的最大 server_seq
     *
     * @param connection 数据库连接
     * @return Map<Long, Long> 键为 conversation_id，值为该会话的最大 server_seq
     * @throws SQLException 数据库操作异常
     */
    public Map<Long, Long> getMaxSequenceIdAllConversation(Connection connection) throws SQLException {
        String sql = "SELECT conversation_id, MAX(server_seq) as max_server_seq FROM messages WHERE user_id = ? GROUP BY conversation_id";
        Map<Long, Long> resultMap = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                long sessionId = rs.getLong("conversation_id");
                long maxSequenceId = rs.getLong("max_server_seq");
                resultMap.put(sessionId, maxSequenceId);
            }
        }

        return resultMap;
    }
}