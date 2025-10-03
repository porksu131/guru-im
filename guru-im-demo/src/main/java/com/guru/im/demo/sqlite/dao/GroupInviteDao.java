package com.guru.im.demo.sqlite.dao;

import com.guru.im.demo.model.GroupInvite;
import com.guru.im.protocol.model.RequestStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupInviteDao {
    private final long userId;

    public GroupInviteDao(long userId) {
        this.userId = userId;
    }

    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS group_invite (" +
                "user_id INTEGER NOT NULL, " +
                "id INTEGER NOT NULL, " +
                "group_id INTEGER NOT NULL, " +
                "inviter_id INTEGER NOT NULL, " +
                "invite_reason TEXT, " +
                "request_status INTEGER NOT NULL DEFAULT 0, " + // 0=PENDING, 1=ACCEPTED, 2=REJECTED, 3=EXPIRED
                "group_name TEXT, " +
                "group_avatar TEXT, " +
                "inviter_name TEXT, " +
                "global_seq INTEGER NOT NULL, " +
                "is_read INTEGER DEFAULT 0, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (user_id, id)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            // 创建关联表存储初始成员列表
            stmt.execute("CREATE TABLE IF NOT EXISTS group_invite_initial_members (" +
                    "user_id INTEGER NOT NULL, " +
                    "invite_id INTEGER NOT NULL, " +
                    "member_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (user_id, invite_id, member_id)" +
                    ")");
            // 创建索引以提高查询性能
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_invite_user_id ON group_invite(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_invite_group_id ON group_invite(user_id, group_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_invite_status ON group_invite(user_id, request_status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_invite_global_seq ON group_invite(user_id, global_seq DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_invite_members_invite_id ON group_invite_initial_members(invite_id);");
        }
    }

    public void saveGroupInvite(Connection connection, GroupInvite invite) throws SQLException {
        String sql = "INSERT OR REPLACE INTO group_invite (user_id, group_id, inviter_id, invite_reason, " +
                "request_status, group_name, group_avatar, inviter_name, global_seq, is_read, id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInviteParameters(pstmt, invite);
            pstmt.executeUpdate();

            // 保存初始成员列表
            saveInitialMembers(connection, invite.getId(), invite.getInitialMembers());
        }
    }

    public void updateGroupInvite(Connection connection, GroupInvite invite) throws SQLException {
        String sql = "UPDATE group_invite SET group_id = ?, inviter_id = ?, invite_reason = ?, " +
                "request_status = ?, group_name = ?, group_avatar = ?, inviter_name = ?, global_seq = ?, " +
                "is_read = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, invite.getGroupId());
            pstmt.setLong(2, invite.getInviterId());
            pstmt.setString(3, invite.getInviteReason());
            pstmt.setInt(4, invite.getRequestStatus().getNumber());
            pstmt.setString(5, invite.getGroupName());
            pstmt.setString(6, invite.getGroupAvatar());
            pstmt.setString(7, invite.getInviterName());
            pstmt.setLong(8, invite.getGlobalSeq());
            pstmt.setInt(9, invite.getIsRead() ? 1 : 0);
            pstmt.setLong(10, userId);
            pstmt.setLong(11, invite.getId());
            pstmt.executeUpdate();

            // 更新初始成员列表
            updateInitialMembers(connection, invite.getId(), invite.getInitialMembers());
        }
    }

    public void updateRequestStatus(Connection connection, int inviteId, RequestStatus status) throws SQLException {
        String sql = "UPDATE group_invite SET request_status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, status.getNumber());
            pstmt.setLong(2, userId);
            pstmt.setInt(3, inviteId);
            pstmt.executeUpdate();
        }
    }

    public void markAsRead(Connection connection, int inviteId) throws SQLException {
        String sql = "UPDATE group_invite SET is_read = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, inviteId);
            pstmt.executeUpdate();
        }
    }

    public void markAllAsRead(Connection connection) throws SQLException {
        String sql = "UPDATE group_invite SET is_read = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND is_read = 0";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        }
    }

    public GroupInvite getGroupInviteById(Connection connection, Long id) throws SQLException {
        String sql = "SELECT * FROM group_invite WHERE user_id = ? AND id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, id);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                GroupInvite invite = resultSetToGroupInvite(rs);
                // 加载初始成员列表
                invite.addAllInitialMembers(getInitialMembers(connection, id));
                return invite;
            }
        }
        return null;
    }

    public List<GroupInvite> getPendingInvites(Connection connection) throws SQLException {
        return getInvitesByStatus(connection, RequestStatus.PENDING);
    }

    public List<GroupInvite> getInvitesByStatus(Connection connection, RequestStatus status) throws SQLException {
        List<GroupInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM group_invite WHERE user_id = ? AND request_status = ? ORDER BY global_seq DESC, id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, status.getNumber());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupInvite invite = resultSetToGroupInvite(rs);
                // 加载初始成员列表
                invite.addAllInitialMembers(getInitialMembers(connection, invite.getId()));
                invites.add(invite);
            }
        }
        return invites;
    }

    public List<GroupInvite> getInvitesByGroupId(Connection connection, long groupId) throws SQLException {
        List<GroupInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM group_invite WHERE user_id = ? AND group_id = ? ORDER BY global_seq DESC, id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupInvite invite = resultSetToGroupInvite(rs);
                // 加载初始成员列表
                invite.addAllInitialMembers(getInitialMembers(connection, invite.getId()));
                invites.add(invite);
            }
        }
        return invites;
    }

    public List<GroupInvite> getUnreadInvites(Connection connection) throws SQLException {
        List<GroupInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM group_invite WHERE user_id = ? AND is_read = 0 ORDER BY global_seq DESC, id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupInvite invite = resultSetToGroupInvite(rs);
                // 加载初始成员列表
                invite.addAllInitialMembers(getInitialMembers(connection, invite.getId()));
                invites.add(invite);
            }
        }
        return invites;
    }

    public List<GroupInvite> getAllInvites(Connection connection) throws SQLException {
        List<GroupInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM group_invite WHERE user_id = ? ORDER BY global_seq DESC, id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupInvite invite = resultSetToGroupInvite(rs);
                // 加载初始成员列表
                invite.addAllInitialMembers(getInitialMembers(connection, invite.getId()));
                invites.add(invite);
            }
        }
        return invites;
    }

    public int getUnreadCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM group_invite WHERE user_id = ? AND is_read = 0";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getPendingCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM group_invite WHERE user_id = ? AND request_status = 0";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public void deleteGroupInvite(Connection connection, Long id) throws SQLException {
        // 先删除关联的初始成员记录
        deleteInitialMembers(connection, id);

        String sql = "DELETE FROM group_invite WHERE user_id = ? AND id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        }
    }

    public void deleteInvitesByGroupId(Connection connection, long groupId) throws SQLException {
        // 先获取要删除的邀请ID
        List<Long> inviteIds = new ArrayList<>();
        String selectSql = "SELECT id FROM group_invite WHERE user_id = ? AND group_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                inviteIds.add(rs.getLong("id"));
            }
        }

        // 删除关联的初始成员记录
        for (Long inviteId : inviteIds) {
            deleteInitialMembers(connection, inviteId);
        }

        // 删除邀请记录
        String deleteSql = "DELETE FROM group_invite WHERE user_id = ? AND group_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, groupId);
            pstmt.executeUpdate();
        }
    }

    // 私有方法：保存初始成员列表
    private void saveInitialMembers(Connection connection, Long inviteId, List<Long> memberIds) throws SQLException {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO group_invite_initial_members (user_id, invite_id, member_id) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Long memberId : memberIds) {
                pstmt.setLong(1, userId);
                pstmt.setLong(2, inviteId);
                pstmt.setLong(3, memberId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    // 私有方法：更新初始成员列表
    private void updateInitialMembers(Connection connection, Long inviteId, List<Long> memberIds) throws SQLException {
        // 先删除旧的成员列表
        deleteInitialMembers(connection, inviteId);
        // 再保存新的成员列表
        saveInitialMembers(connection, inviteId, memberIds);
    }

    // 私有方法：获取初始成员列表
    private List<Long> getInitialMembers(Connection connection, Long inviteId) throws SQLException {
        List<Long> memberIds = new ArrayList<>();
        String sql = "SELECT member_id FROM group_invite_initial_members WHERE user_id = ? AND invite_id = ? ORDER BY member_id";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, inviteId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                memberIds.add(rs.getLong("member_id"));
            }
        }
        return memberIds;
    }

    // 私有方法：删除初始成员列表
    private void deleteInitialMembers(Connection connection, Long inviteId) throws SQLException {
        String sql = "DELETE FROM group_invite_initial_members WHERE user_id = ? AND invite_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, inviteId);
            pstmt.executeUpdate();
        }
    }

    private GroupInvite resultSetToGroupInvite(ResultSet rs) throws SQLException {
        GroupInvite invite = new GroupInvite();
        invite.setId(rs.getLong("id"));
        invite.setGroupId(rs.getLong("group_id"));
        invite.setInviterId(rs.getLong("inviter_id"));
        invite.setInviteReason(rs.getString("invite_reason"));
        invite.setRequestStatus(RequestStatus.forNumber(rs.getInt("request_status")));
        invite.setGroupName(rs.getString("group_name"));
        invite.setGroupAvatar(rs.getString("group_avatar"));
        invite.setInviterName(rs.getString("inviter_name"));
        invite.setGlobalSeq(rs.getLong("global_seq"));
        invite.setIsRead(rs.getInt("is_read") == 1);

        return invite;
    }

    private void setInviteParameters(PreparedStatement pstmt, GroupInvite invite) throws SQLException {
        pstmt.setLong(1, userId);
        pstmt.setLong(2, invite.getGroupId());
        pstmt.setLong(3, invite.getInviterId());
        pstmt.setString(4, invite.getInviteReason());
        pstmt.setInt(5, invite.getRequestStatus().getNumber());
        pstmt.setString(6, invite.getGroupName());
        pstmt.setString(7, invite.getGroupAvatar());
        pstmt.setString(8, invite.getInviterName());
        pstmt.setLong(9, invite.getGlobalSeq());
        pstmt.setInt(10, invite.getIsRead() ? 1 : 0);
        pstmt.setLong(11, invite.getId());
    }
}