package com.guru.im.demo.sqlite;

import com.guru.im.demo.gui.file.model.FileTransfer;
import com.guru.im.demo.model.*;
import com.guru.im.demo.sqlite.dao.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private final long userId;
    private final MessageDao messageManager;
    private final FriendRequestDao friendRequestManager;
    private final UserConversationDao conversationManager;
    private final SyncStateDao syncStateManager;
    private final FileTransferDao fileTransferManager;
    private final NotificationDao notificationManager;
    private final GroupInviteDao groupInviteManager;

    public DatabaseManager(UserInfo currentUser) {
        this.userId = currentUser.getUid();
        this.messageManager = new MessageDao(userId);
        this.friendRequestManager = new FriendRequestDao(userId);
        this.conversationManager = new UserConversationDao(userId);
        this.syncStateManager = new SyncStateDao(userId);
        this.fileTransferManager = new FileTransferDao(userId);
        this.notificationManager = new NotificationDao(userId);
        this.groupInviteManager = new GroupInviteDao(userId);
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection connection = DBConnectManager.getConnection()) {
            MessageDao.createTable(connection);
            FriendRequestDao.createTable(connection);
            UserConversationDao.createTable(connection);
            SyncStateDao.createTable(connection);
            FileTransferDao.createTable(connection);
            NotificationDao.createTable(connection);
            GroupInviteDao.createTable(connection);
        } catch (SQLException e) {
            System.out.println("DatabaseManager 初始化失败：" + e.getMessage());
        }
    }

    // 消息相关方法
    public void saveMessage(Message message) {
        try (Connection connection = DBConnectManager.getConnection()) {
            messageManager.saveMessage(connection, message);
        } catch (SQLException e) {
            System.err.println("保存消息时出错: " + e.getMessage());
        }
    }

    // 查询指定会话中最大序列号对应的消息
    public Message getMaxSequenceMessageByConversation(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getMaxSequenceMessageByConversation(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("查询指定会话中最大序列号对应的消息出错: " + e.getMessage());
            return null;
        }
    }

    // 查询指定会话中当前用户发送的未读消息数量
    public int getUnreadCountSentByMe(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getUnreadCountSentByMe(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("查询指定会话中当前用户发送的未读消息数量出错: " + e.getMessage());
            return 0;
        }
    }

    public long getMaxSequenceId(Long sessionId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getMaxSequenceId(connection, sessionId);
        } catch (SQLException e) {
            System.err.println("获取最大序列号id出错: " + e.getMessage());
            return 0;
        }
    }

    public long getMaxClientSeq() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getMaxClientSeq(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<Message> loadSendFailMessages(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.loadSendFailMessages(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("加载消息时出错: " + e.getMessage());
            return null;
        }
    }

    public Map<Long, Long> getMaxSequenceIdAllConversation() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getMaxSequenceIdAllConversation(connection);
        } catch (SQLException e) {
            System.err.println("加载消息时出错: " + e.getMessage());
            return null;
        }
    }

    public List<Message> loadMessages(Long conversationId, long afterSequenceId, int limit) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.loadMessages(connection, conversationId, afterSequenceId, limit);
        } catch (SQLException e) {
            System.err.println("加载消息时出错: " + e.getMessage());
            return null;
        }
    }

    public int updateMessagesToReadBySeq(long conversationId, long readSeq) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.updateMessagesToReadBySeq(connection, conversationId, readSeq);
        } catch (SQLException e) {
            System.err.println("根据序列号更新消息为已读状态时出错: " + e.getMessage());
            return 0;
        }
    }

    public int getNotificationUnreadCount(long peerId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return messageManager.getUnreadCount(connection, peerId);
        } catch (SQLException e) {
            System.err.println("获取未读消息数时出错: " + e.getMessage());
            return 0;
        }
    }

    public void batchUpdateStatus(List<Long> messageIds, int newStatus) {
        try (Connection connection = DBConnectManager.getConnection()) {
            messageManager.batchUpdateStatus(connection, messageIds, newStatus);
        } catch (SQLException e) {
            System.err.println("批量更新状态时出错: " + e.getMessage());
        }
    }

    public void deleteMessage(Message message) {
        try (Connection connection = DBConnectManager.getConnection()) {
            messageManager.deleteMessage(connection, message);
        } catch (SQLException e) {
            System.err.println("删除消息时出错: " + e.getMessage());
        }
    }

    public void deleteAllConversationMessage(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            messageManager.deleteAllConversationMessage(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("删除会话消息时出错: " + e.getMessage());
        }
    }


    // 好友请求相关方法
    public boolean saveFriendRequest(FriendRequest request) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.saveFriendRequest(connection, request);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<FriendRequest> getPendingRequests() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.getPendingRequests(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public FriendRequest getFriendRequestById(Long id) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.getFriendRequestById(connection, id);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<FriendRequest> getAllFriendRequests() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.getAllFriendRequests(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public int getFriendRequestCount(long requesterId, long responseId, int requestStatus, int requestType) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.getFriendRequestCount(connection, requesterId, responseId, requestStatus, requestType);
        } catch (SQLException e) {
            System.err.println("获取好友请求数量时出错: " + e.getMessage());
            return 0;
        }
    }

    public boolean updateRequestStatus(long id, int status, Long responseTime, Long responderId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return friendRequestManager.updateRequestStatus(connection, id, status, responseTime, responderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 用户会话相关方法
    public boolean saveOrUpdateUserConversation(UserConversation conversation) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.saveOrUpdateUserConversation(connection, conversation);
        } catch (SQLException e) {
            System.err.println("保存或更新用户会话时出错: " + e.getMessage());
            return false;
        }
    }

    public List<UserConversation> getUserConversations(Long userId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.getUserConversations(connection);
        } catch (SQLException e) {
            System.err.println("获取用户会话列表时出错: " + e.getMessage());
            return List.of();
        }
    }

    public UserConversation getUserConversation(Long userId, Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.getUserConversation(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("获取特定会话时出错: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteUserConversation(Long userId, Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.deleteUserConversation(connection, conversationId);
        } catch (SQLException e) {
            System.err.println("删除会话时出错: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUnreadCount(Long userId, Long conversationId, int unreadCount) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.updateUnreadCount(connection, conversationId, unreadCount);
        } catch (SQLException e) {
            System.err.println("更新未读消息数时出错: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTopStatus(Long userId, Long conversationId, boolean isTop) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.updateTopStatus(connection, conversationId, isTop);
        } catch (SQLException e) {
            System.err.println("更新置顶状态时出错: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMuteStatus(Long userId, Long conversationId, boolean isMute) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.updateMuteStatus(connection, conversationId, isMute);
        } catch (SQLException e) {
            System.err.println("更新免打扰状态时出错: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLastReadSeq(Long userId, Long conversationId, Long lastReadSeq) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.updateLastReadSeq(connection, conversationId, lastReadSeq);
        } catch (SQLException e) {
            System.err.println("更新最后阅读序列号时出错: " + e.getMessage());
            return false;
        }
    }

    public void batchSaveUserConversations(List<UserConversation> conversations) {
        try (Connection connection = DBConnectManager.getConnection()) {
            conversationManager.batchSaveUserConversations(connection, conversations);
        } catch (SQLException e) {
            System.err.println("批量保存会话列表时出错: " + e.getMessage());
        }
    }

    public boolean clearUserConversations(Long userId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return conversationManager.clearUserConversations(connection);
        } catch (SQLException e) {
            System.err.println("清除用户会话时出错: " + e.getMessage());
            return false;
        }
    }

    // 同步状态相关方法
    public long getLastSyncSeq(Long userId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return syncStateManager.getLastSyncSeq(connection);
        } catch (SQLException e) {
            throw new RuntimeException("获取最后同步序列号失败", e);
        }
    }

    public void updateLastSyncSeq(Long userId, long seq) {
        try (Connection connection = DBConnectManager.getConnection()) {
            syncStateManager.updateLastSyncSeq(connection, seq);
        } catch (SQLException e) {
            throw new RuntimeException("更新同步序列号失败", e);
        }
    }

    // 文件传输相关方法
    public void saveFileTransfer(FileTransfer transfer) {
        try (Connection connection = DBConnectManager.getConnection()) {
            fileTransferManager.saveFileTransfer(connection, transfer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFileTransfer(FileTransfer transfer) {
        try (Connection connection = DBConnectManager.getConnection()) {
            fileTransferManager.updateFileTransfer(connection, transfer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FileTransfer> getPendingTransfersByConversation(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return fileTransferManager.getPendingTransfersByConversation(connection, conversationId);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public FileTransfer getFileTransferByClientMsgId(String clientMsgId, int transferType) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return fileTransferManager.getFileTransferByClientMsgId(connection, clientMsgId, transferType);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取指定会话的所有文件传输
    public List<FileTransfer> getFileTransfersByConversation(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return fileTransferManager.getFileTransfersByConversationId(connection, conversationId);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // 删除文件传输记录
    public boolean deleteFileTransfer(String transferId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            fileTransferManager.deleteFileTransfer(connection, transferId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 清除会话下的所有文件历史传输记录
    public boolean deleteAllConversationFileTransfer(Long conversationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            fileTransferManager.deleteAllConversationFileTransfer(connection, conversationId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // 获取所有未完成的文件传输
    public Map<Long, List<FileTransfer>> getAllPendingTransfers() {
        try (Connection connection = DBConnectManager.getConnection()) {
            // 这个方法需要在FileTransferTableManager中实现
            // 由于原代码有这个功能，我们需要在FileTransferTableManager中添加
            return fileTransferManager.getAllPendingTransfers(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    // 保存系统通知
    public void saveNotification(Notification notification) {
        try (Connection connection = DBConnectManager.getConnection()) {
            notificationManager.saveNotification(connection, notification);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 获取所有系统通知
    public List<Notification> getAllNotifications() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return notificationManager.getAllNotifications(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // 获取通知的未读数量
    public int getNotificationUnreadCount() {
        try (Connection connection = DBConnectManager.getConnection()) {
            return notificationManager.getUnreadCount(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 标记通知为已读
    public void markNotificationAsRead(Long notificationId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            notificationManager.markAsRead(connection, notificationId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 保存群聊邀请
    public void saveGroupInvite(GroupInvite groupInvite) {
        try (Connection connection = DBConnectManager.getConnection()) {
            groupInviteManager.saveGroupInvite(connection, groupInvite);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 根据ID获取群邀请
    public GroupInvite getGroupInviteById(Long groupInviteId) {
        try (Connection connection = DBConnectManager.getConnection()) {
            return groupInviteManager.getGroupInviteById(connection, groupInviteId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public void close() {
        // 连接由各个方法自行管理，不需要额外关闭
    }


}