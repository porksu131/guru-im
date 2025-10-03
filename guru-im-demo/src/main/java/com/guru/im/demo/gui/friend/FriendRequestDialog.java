package com.guru.im.demo.gui.friend;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.model.FriendRequest;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.demo.sqlite.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class FriendRequestDialog extends JDialog {
    // 颜色定义
    private final Color SUCCESS_COLOR = new Color(52, 199, 89);
    private final Color DANGER_COLOR = new Color(255, 59, 48);
    private final Color DEAL_COLOR = new Color(182, 180, 180);
    private final Color CARD_COLOR = Color.WHITE;

    // 卡片尺寸常量
    private static final int CARD_WIDTH = 340;
    private static final int CARD_HEIGHT = 120;

    private final DatabaseManager databaseManager;
    private final MainFrame mainFrame;

    private final UserInfo currentUser;
    private JPanel mainPanel;
    private int pendingCount;

    public FriendRequestDialog(MainFrame mainFrame) {
        super(mainFrame, "新的好友", true);
        this.currentUser = mainFrame.getCurrentUser();
        this.databaseManager = mainFrame.getDatabaseManager();
        this.mainFrame = mainFrame;
        initializeDialog();
        loadRequests();
    }

    private void initializeDialog() {
        setSize(400, 450);
        setLocationRelativeTo(mainFrame);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        // 主面板 - 使用垂直盒布局
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 添加顶部填充，使内容从顶部开始
        mainPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        // 底部按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JButton closeButton = new JButton("关闭");
        styleButton(closeButton, closeButton.getBackground(), Color.BLACK);
        closeButton.addActionListener(e -> dispose());

        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadRequests() {
        mainPanel.removeAll();

        List<FriendRequest> allFriendRequests = databaseManager.getAllFriendRequests().stream()
                .sorted((o1, o2) -> {
                    // 如果o1是PENDING而o2不是，o1应该排在前面（返回负数）
                    boolean o1IsPending = o1.getRequestStatus() == FriendRequest.RequestStatus.PENDING.getValue();
                    boolean o2IsPending = o2.getRequestStatus() == FriendRequest.RequestStatus.PENDING.getValue();

                    if (o1IsPending && !o2IsPending) {
                        return -1;
                    } else if (!o1IsPending && o2IsPending) {
                        return 1;
                    } else {
                        // 如果两者状态相同（都是PENDING或都不是），则按照创建时间倒序排序
                        return o2.getCreateTime().compareTo(o1.getCreateTime());
                    }
                })
                .toList();
        this.pendingCount = (int) allFriendRequests.stream()
                .filter(obj->obj.getRequestStatus() == FriendRequest.RequestStatus.PENDING.getValue())
                .count();

        if (allFriendRequests.isEmpty()) {
            JLabel emptyLabel = new JLabel("暂无好友申请记录");
            emptyLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(142, 142, 147));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(new EmptyBorder(100, 0, 0, 0));
            mainPanel.add(emptyLabel);
        } else {
            for (FriendRequest request : allFriendRequests) {
                JPanel card = createRequestCard(request);
                // 设置卡片固定大小
                card.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
                card.setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
                card.setMinimumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

                mainPanel.add(card);
                mainPanel.add(Box.createVerticalStrut(10));
            }
        }

        // 添加底部填充，使内容居中
        mainPanel.add(Box.createVerticalGlue());

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void refreshParentPending() {
        mainFrame.updateNotificationUnreadCount();
    }

    private JPanel createRequestCard(FriendRequest request) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // 左侧信息
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(request.getRequesterName());
        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));

        JLabel reasonLabel = new JLabel("<html><div style='width: 200px;'>" +
                request.getRequestMsg() + "</div></html>");
        reasonLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        reasonLabel.setForeground(new Color(142, 142, 147));

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(reasonLabel);

        // 右侧时间
        JLabel timeLabel = new JLabel(formatRelativeTime(request.getCreateTime()));
        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(142, 142, 147));

        // 操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        if(request.getRequestStatus() ==  FriendRequest.RequestStatus.ACCEPTED.getValue()){
            JButton dealBtn = createActionButton("已同意", DEAL_COLOR, request, true);
            dealBtn.setEnabled(false);
            buttonPanel.add(dealBtn);
        }else if (request.getRequestStatus() ==  FriendRequest.RequestStatus.REJECTED.getValue()){
            JButton dealBtn = createActionButton("已拒绝", DEAL_COLOR, request, true);
            dealBtn.setEnabled(false);
            buttonPanel.add(dealBtn);
        }else {
            JButton rejectBtn = createActionButton("拒绝", DANGER_COLOR, request, false);
            JButton acceptBtn = createActionButton("同意", SUCCESS_COLOR, request, true);
            buttonPanel.add(rejectBtn);
            buttonPanel.add(acceptBtn);
        }

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(timeLabel, BorderLayout.EAST);

        contentPanel.add(topPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    private JButton createActionButton(String text, Color color, FriendRequest request, boolean accept) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            processRequest(request, accept);
            loadRequests(); // 刷新列表
            refreshParentPending();
        });

        return button;
    }

    private void processRequest(FriendRequest request, boolean accept) {
        FriendRequest.RequestStatus status = accept ?
                FriendRequest.RequestStatus.ACCEPTED :
                FriendRequest.RequestStatus.REJECTED;

        try {
            ResponseResult<Void> sendResult = ApiService.sendFriendResponse(currentUser.getUid(), request, accept, currentUser.getAccessToken());
            if (ResponseResult.isSuccess(sendResult)) {
                String message = accept ? "已同意 " + request.getRequesterName() + " 的好友申请" :
                        "已拒绝 " + request.getRequesterName() + " 的好友申请";
                JOptionPane.showMessageDialog(this, message, "成功", JOptionPane.INFORMATION_MESSAGE);
                try {
                    databaseManager.updateRequestStatus(request.getId(), status.getValue(),
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
    }

    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        if (bgColor == null) {
            button.setBackground(bgColor);
        }
        button.setForeground(fgColor);
        button.setPreferredSize(new Dimension(80, 30));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private String formatRelativeTime(long timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;

        if (diff < 60) return "刚刚";
        if (diff < 3600) return (diff / 60) + "分钟前";
        if (diff < 86400) return (diff / 3600) + "小时前";
        if (diff < 2592000) return (diff / 86400) + "天前";

        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(new java.util.Date(timestamp * 1000));
    }

    public int getPendingCount() {
        return pendingCount;
    }
}