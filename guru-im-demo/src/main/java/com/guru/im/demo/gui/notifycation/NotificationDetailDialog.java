package com.guru.im.demo.gui.notifycation;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.FriendRequest;
import com.guru.im.demo.model.GroupInvite;
import com.guru.im.demo.model.Notification;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.protocol.model.RequestStatus;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

// 通知详情对话框
public class NotificationDetailDialog extends JDialog {
    private final Notification notification;
    private Runnable refreshCallback;
    private final MainFrame mainFrame;
    private final UserInfo currentUser;

    public NotificationDetailDialog(Notification notification, MainFrame mainFrame) {
        super(mainFrame, "通知详情", true);
        this.notification = notification;
        this.mainFrame = mainFrame;
        this.currentUser = mainFrame.getCurrentUser();
        initUI();
        setLocationRelativeTo(mainFrame);
    }

    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setSize(450, 500);
        setResizable(false);

        // 使用渐变背景
        GradientPanel mainPanel = new GradientPanel(
                new Color(248, 250, 252),
                new Color(241, 245, 249)
        );
        mainPanel.setLayout(new BorderLayout());

        // 头部区域
        JPanel headerPanel = createHeaderPanel();

        // 内容区域
        JPanel contentPanel = createContentPanel();

        // 操作按钮区域
        JPanel actionPanel = createActionPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new RoundedPanel(0);
        headerPanel.setLayout(new BorderLayout(15, 15));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));
        headerPanel.setBackground(Color.WHITE);

        // 类型图标和标题
        JPanel titlePanel = new JPanel(new BorderLayout(10, 5));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(getTypeIcon(notification.getTypeEnum(), 40));
        JLabel titleLabel = new JLabel(notification.getTitle());
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(new Color(30, 41, 59));

        titlePanel.add(iconLabel, BorderLayout.WEST);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // 发送者和时间
        JPanel infoPanel = new JPanel(new BorderLayout(5, 2));
        infoPanel.setOpaque(false);

        JLabel senderLabel = new JLabel("来自: " + notification.getSenderName(currentUser.getUid()));
        senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        senderLabel.setForeground(new Color(100, 116, 139));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        JLabel timeLabel = new JLabel(sdf.format(notification.getTimestamp()));
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(148, 163, 184));

        infoPanel.add(senderLabel, BorderLayout.NORTH);
        infoPanel.add(timeLabel, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.SOUTH);

        return headerPanel;
    }


    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setOpaque(false);

        JTextArea contentArea = new JTextArea(getContent());
        contentArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);
        contentArea.setForeground(new Color(71, 85, 105));

        // 创建带圆角的滚动面板
        JPanel scrollWrapper = new RoundedPanel(10);
        scrollWrapper.setLayout(new BorderLayout());
        scrollWrapper.setBackground(Color.WHITE);
        scrollWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        scrollWrapper.add(contentArea, BorderLayout.CENTER);

        contentPanel.add(scrollWrapper, BorderLayout.CENTER);

        return contentPanel;
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new BorderLayout(10, 15));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        actionPanel.setOpaque(false);

        // 关闭按钮
        ModernButton closeButton = new ModernButton("关闭",
                new Color(241, 245, 249),
                new Color(226, 232, 240),
                new Color(71, 85, 105)
        );
        closeButton.addActionListener(e -> dispose());

        actionPanel.add(closeButton, BorderLayout.CENTER);

        putResultLabelOrPutActionButton(actionPanel);

        return actionPanel;
    }

    private void putResultLabelOrPutActionButton(JPanel actionPanel) {
        JLabel resultLabel = new JLabel();
        resultLabel.setFont(new Font("微软雅黑", Font.ITALIC, 12));
        resultLabel.setForeground(new Color(100, 116, 139));
        // 好友申请
        if (notification.getType() == Notification.Type.FRIEND_REQUEST.getCode()) {
            FriendRequest friendRequest = notification.getFriendRequest();
            if (friendRequest.getRequesterId() == currentUser.getUid()) {
                // 我发送的好友请求
                if (friendRequest.getRequestStatus() == RequestStatus.PENDING_VALUE) {
                    resultLabel.setText("处理结果：您的好友申请已发送，待“" + friendRequest.getResponderName() + "”同意");
                    actionPanel.add(resultLabel, BorderLayout.NORTH);
                } else if (friendRequest.getRequestStatus() == RequestStatus.ACCEPTED_VALUE) {
                    resultLabel.setText("处理结果：“" + friendRequest.getResponderName() + "”同意了您的好友申请");
                    actionPanel.add(resultLabel, BorderLayout.NORTH);
                } else if (friendRequest.getRequestStatus() == RequestStatus.REJECTED_VALUE) {
                    resultLabel.setText("处理结果：“" + friendRequest.getResponderName() + "”拒绝了您的好友申请");
                    actionPanel.add(resultLabel, BorderLayout.NORTH);
                }
            } else {
                // 别人发送的好友请求
                if (friendRequest.getRequestStatus() == RequestStatus.PENDING_VALUE) {
                    putActionButtons(actionPanel); // 待处理状态，需要显示按钮
                } else if (friendRequest.getRequestStatus() == RequestStatus.ACCEPTED_VALUE) {
                    resultLabel.setText("处理结果：您已同意此好友请求");
                    actionPanel.add(resultLabel, BorderLayout.NORTH);
                } else if (friendRequest.getRequestStatus() == RequestStatus.REJECTED_VALUE) {
                    resultLabel.setText("处理结果：您已拒绝此好友请求");
                    actionPanel.add(resultLabel, BorderLayout.NORTH);
                }
            }
        }
        // 群聊邀请，当前就只有默认同意
        else if (notification.getType() == Notification.Type.GROUP_INVITE.getCode()) {
            if (notification.getGroupInvite().getRequestStatus() == RequestStatus.PENDING) {
                putActionButtons(actionPanel); // 待处理状态，需要显示按钮
            } else if (notification.getGroupInvite().getRequestStatus() == RequestStatus.ACCEPTED) {
                resultLabel.setText("您已加入此群聊");
                actionPanel.add(resultLabel, BorderLayout.NORTH);
            } else if (notification.getGroupInvite().getRequestStatus() == RequestStatus.REJECTED) {
                resultLabel.setText("您已拒绝加入此群聊");
                actionPanel.add(resultLabel, BorderLayout.NORTH);
            }
        }
    }

    private void putActionButtons(JPanel actionPanel) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        ModernButton rejectButton = new ModernButton("拒绝",
                new Color(254, 242, 242),
                new Color(254, 226, 226),
                new Color(220, 38, 38)
        );
        rejectButton.addActionListener(e -> {
            if (notification.getType() == 2) {
                handleFriendRequest(notification.getFriendRequest(), false);
            } else if (notification.getType() == 3) {
                handleGroupInvite(notification.getGroupInvite(), false);
            }

        });

        ModernButton acceptButton = new ModernButton("同意",
                new Color(236, 253, 245),
                new Color(209, 250, 229),
                new Color(5, 122, 85)
        );
        acceptButton.addActionListener(e -> {
            if (notification.getType() == 2) {
                handleFriendRequest(notification.getFriendRequest(), true);
            } else if (notification.getType() == 3) {
                handleGroupInvite(notification.getGroupInvite(), true);
            }
        });

        buttonPanel.add(rejectButton);
        buttonPanel.add(acceptButton);
        actionPanel.add(buttonPanel, BorderLayout.EAST);
    }

    private String getContent() {
        if (notification.getType() == Notification.Type.FRIEND_REQUEST.getCode()) {
            if (StringUtils.isNotBlank(notification.getFriendRequest().getRequestMsg())) {
                return notification.getFriendRequest().getRequestMsg();
            }
        } else if (notification.getType() == Notification.Type.GROUP_INVITE.getCode()) {
            if (StringUtils.isNotBlank(notification.getGroupInvite().getInviteReason())) {
                return notification.getGroupInvite().getInviteReason();
            }
        }
        return notification.getContent();
    }

    private Icon getTypeIcon(Notification.Type type, int size) {
        Color color;
        String symbol;

        switch (type) {
            case SYS_UPDATE:
                color = new Color(0, 120, 215);
                symbol = "⚙";
                break;
            case FRIEND_REQUEST:
                color = new Color(16, 137, 62);
                symbol = "👤";
                break;
            case GROUP_INVITE:
                color = new Color(216, 59, 1);
                symbol = "👥";
                break;
            case SECURITY_NOTIFY:
                color = new Color(200, 0, 0);
                symbol = "🔒";
                break;
            case BIRTHDAY_NOTIFY:
                color = new Color(180, 0, 158);
                symbol = "🎂";
                break;
            default:
                color = Color.GRAY;
                symbol = "📢";
        }

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制圆形背景
                g2.setColor(color);
                g2.fillOval(x, y, size, size);

                // 绘制符号
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("微软雅黑 Emoji", Font.PLAIN, size / 2));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(symbol);
                int textHeight = fm.getHeight();
                g2.drawString(symbol, x + size / 2 - textWidth / 2, y + size / 2 + textHeight / 4 + 2);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private void handleFriendRequest(FriendRequest request, boolean accept) {

        try {
            FriendRequest.RequestStatus status = accept ? FriendRequest.RequestStatus.ACCEPTED :
                    FriendRequest.RequestStatus.REJECTED;
            UserInfo currentUser = mainFrame.getCurrentUser();
            ResponseResult<Void> sendResult = ApiService.sendFriendResponse(currentUser.getUid(), request, accept, currentUser.getAccessToken());
            if (ResponseResult.isSuccess(sendResult)) {
                String message = accept ? "已同意 " + request.getRequesterName() + " 的好友申请" :
                        "已拒绝 " + request.getRequesterName() + " 的好友申请";
                JOptionPane.showMessageDialog(this, message, "成功", JOptionPane.INFORMATION_MESSAGE);
                try {
                    mainFrame.getDatabaseManager().updateRequestStatus(request.getId(), status.getValue(),
                            System.currentTimeMillis(), currentUser.getUid()); // 提交成功后更新本地
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "操作失败，请重试", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, sendResult.getMsg(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "处理失败:" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
        }

        if (refreshCallback != null) {
            refreshCallback.run();
        }
        dispose();
    }

    private void handleGroupInvite(GroupInvite groupInvite, boolean accepted) {
        String message = accepted ? "已加入群聊" : "已拒绝群聊邀请";
        JOptionPane.showMessageDialog(this, message, "操作成功", JOptionPane.INFORMATION_MESSAGE);

        // 目前群聊邀请都是默认已同意

        if (refreshCallback != null) {
            refreshCallback.run();
        }
        dispose();
    }
}

