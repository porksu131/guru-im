package com.guru.im.demo.gui.notifycation;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.FriendRequest;
import com.guru.im.demo.model.GroupInvite;
import com.guru.im.demo.model.Notification;
import com.guru.im.demo.util.TimeUtil;
import com.guru.im.protocol.model.RequestStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 通知消息卡片组件
public class NotificationCard extends JPanel {
    private Notification notification;
    private MainFrame mainFrame;
    private NotificationListDialog notificationListDialog;
    private JLabel statusLabel;
    private JLabel unreadIndicator;

    public NotificationCard(NotificationListDialog notificationListDialog, Notification notification, MainFrame mainFrame) {
        this.notification = notification;
        this.mainFrame = mainFrame;
        this.notificationListDialog = notificationListDialog;
        initUI();
        loadDetail();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 5));
        setBackground(notification.getRead() ? new Color(245, 245, 245) : new Color(230, 240, 255));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        setPreferredSize(new Dimension(360, 60));
        setMinimumSize(new Dimension(360, 60));
        setMaximumSize(new Dimension(360, 60));

        // 左侧图标
        JLabel iconLabel = new JLabel(getIconForType(notification.getTypeEnum()));
        iconLabel.setPreferredSize(new Dimension(40, 40));

        // 中间内容区域
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(notification.getTitle());
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));

        JLabel contentLabel = new JLabel(notification.getContent());
        contentLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        contentLabel.setForeground(Color.GRAY);

        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(contentLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder());

        // 右侧时间标签
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        timePanel.setOpaque(false);
        timePanel.setBorder(BorderFactory.createEmptyBorder());
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setText(TimeUtil.formatTime(notification.getTimestamp()));

        // 未读指示器
        unreadIndicator = new JLabel("●");
        unreadIndicator.setForeground(new Color(0, 120, 215));
        unreadIndicator.setFont(new Font("微软雅黑", Font.BOLD, 10));

        timePanel.add(unreadIndicator);
        timePanel.add(timeLabel);
        if (!notification.getRead()) {
            unreadIndicator.setVisible(true);
        } else {
            unreadIndicator.setVisible(false);
        }
        rightPanel.add(timePanel, BorderLayout.NORTH);

        // 右侧，处理状态
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder());
        statusLabel = new JLabel();
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setOpaque(false);
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        if (!notification.getRead()) {
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        }
        statusPanel.add(statusLabel);

        rightPanel.add(statusPanel, BorderLayout.CENTER);

        add(iconLabel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // 添加点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openNotificationDetail();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new Color(240, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(notification.getRead() ? new Color(245, 245, 245) : new Color(230, 240, 255));
            }
        });
    }

    private String getStatusText() {
        if (notification.getType() == Notification.Type.FRIEND_REQUEST.getCode()) {
            // 好友请求
            FriendRequest friendRequest = notification.getFriendRequest();
            if (friendRequest.getRequesterId() == this.mainFrame.getCurrentUser().getUid()) {
                if (friendRequest.getRequestStatus() == RequestStatus.PENDING_VALUE) {
                    return "待同意";
                } else if (friendRequest.getRequestStatus() == RequestStatus.REJECTED_VALUE) {
                    return "被拒绝";
                } else if (friendRequest.getRequestStatus() == RequestStatus.ACCEPTED_VALUE) {
                    return "已添加";
                }
            } else {
                if (friendRequest.getRequestStatus() == RequestStatus.PENDING_VALUE) {
                    return "待处理";
                } else if (friendRequest.getRequestStatus() == RequestStatus.REJECTED_VALUE) {
                    return "已拒绝";
                } else if (friendRequest.getRequestStatus() == RequestStatus.ACCEPTED_VALUE) {
                    return "已同意";
                }
            }
        }

        // 群聊，默认已加入
        if (notification.getType() == Notification.Type.GROUP_INVITE.getCode()) {
            if (notification.getGroupInvite().getRequestStatus() == RequestStatus.ACCEPTED) {
                return "已加入";
            }
            return "";
        }

        return "";
    }

    private Color getStatusColor() {
        int status = -1;
        if (notification.getType() == Notification.Type.GROUP_INVITE.getCode()) {
            status = notification.getGroupInvite().getRequestStatus().getNumber();
        } else if (notification.getType() == Notification.Type.FRIEND_REQUEST.getCode()) {
            status = notification.getFriendRequest().getRequestStatus();
        }
        if (status == RequestStatus.PENDING_VALUE) {
            return new Color(244, 172, 126);
        } else if (status == RequestStatus.REJECTED_VALUE) {
            return new Color(236, 102, 124);
        } else if (status == RequestStatus.ACCEPTED_VALUE) {
            return new Color(88, 209, 134);
        }

        return Color.GRAY;
    }

    private void loadDetail() {
        if (notification.getType() == Notification.Type.FRIEND_REQUEST.getCode()) {
            // 好友请求
            FriendRequest friendRequest = mainFrame.getDatabaseManager().getFriendRequestById(notification.getCorrelationId());
            notification.setFriendRequest(friendRequest);

        } else if (notification.getType() == Notification.Type.GROUP_INVITE.getCode()) {
            // 群邀请
            GroupInvite groupInvite = mainFrame.getDatabaseManager().getGroupInviteById(notification.getCorrelationId());
            notification.setGroupInvite(groupInvite);
        }

        // 更新处理状态的展示
        statusLabel.setText(getStatusText());
        statusLabel.setForeground(getStatusColor());
    }

    private Icon getIconForType(Notification.Type type) {
        // 这里可以使用实际图标，这里用文字代替
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(30, 30));
        icon.setOpaque(true);
        icon.setHorizontalAlignment(JLabel.CENTER);
        icon.setForeground(Color.WHITE);
        icon.setFont(new Font("微软雅黑", Font.BOLD, 12));

        switch (type) {
            case SYS_UPDATE:
                icon.setBackground(new Color(0, 120, 215));
                icon.setText("更");
                break;
            case FRIEND_REQUEST:
                icon.setBackground(new Color(16, 137, 62));
                icon.setText("友");
                break;
            case GROUP_INVITE:
                icon.setBackground(new Color(216, 59, 1));
                icon.setText("群");
                break;
            case SECURITY_NOTIFY:
                icon.setBackground(new Color(200, 0, 0));
                icon.setText("安");
                break;
            case BIRTHDAY_NOTIFY:
                icon.setBackground(new Color(180, 0, 158));
                icon.setText("生");
                break;
            default:
                icon.setBackground(Color.GRAY);
                icon.setText("通");
        }

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(icon.getBackground());
                g2.fillOval(x, y, 30, 30);
                g2.setColor(Color.WHITE);
                g2.setFont(icon.getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(icon.getText());
                int textHeight = fm.getHeight();
                g2.drawString(icon.getText(), x + 15 - textWidth / 2, y + 15 + textHeight / 4);
            }

            @Override
            public int getIconWidth() {
                return 30;
            }

            @Override
            public int getIconHeight() {
                return 30;
            }
        };
    }

    private void openNotificationDetail() {
        // 标记为已读
        if (!notification.getRead()) {
            this.mainFrame.getDatabaseManager().markNotificationAsRead(notification.getId());
            notification.setRead(true);
            if (unreadIndicator != null) {
                unreadIndicator.setVisible(false);
            }
            this.mainFrame.updateNotificationUnreadCount();
            setBackground(new Color(245, 245, 245));
            repaint();
        }

        // 打开详情页面
        NotificationDetailDialog detailDialog = new NotificationDetailDialog(notification, mainFrame);
        detailDialog.setRefreshCallback(() -> this.notificationListDialog.refreshNotifications());
        detailDialog.setVisible(true);
    }
}

