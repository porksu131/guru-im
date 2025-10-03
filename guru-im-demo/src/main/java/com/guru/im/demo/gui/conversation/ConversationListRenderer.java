package com.guru.im.demo.gui.conversation;

import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.util.ImageUtil;
import com.guru.im.demo.util.TimeUtil;
import com.guru.im.protocol.model.ConversationType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ConversationListRenderer extends JPanel implements ListCellRenderer<UserConversation> {
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel contentLabel;
    private JLabel timeLabel;
    private JLabel topIconLabel;
    private JLabel muteIconLabel;
    private JLabel unreadLabel;

    private final Color selectedColor = new Color(240, 242, 245);
    private final Color hoverColor = new Color(245, 245, 245);
    private final Color normalColor = Color.WHITE;
    private final Color unreadColor = new Color(0, 150, 255);
    private final Font nameFont = new Font("Microsoft YaHei", Font.BOLD, 12);
    private final Font contentFont = new Font("Microsoft YaHei", Font.PLAIN, 11);
    private final Font timeFont = new Font("Microsoft YaHei", Font.PLAIN, 11);
    private final Font unreadFont = new Font("Microsoft YaHei", Font.BOLD, 10);

    public ConversationListRenderer() {
        setLayout(new BorderLayout(10, 5));
        setBorder(new EmptyBorder(8, 5, 8, 5));
        setOpaque(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        initComponents();
        setupLayout();
    }

    private void initComponents() {
        // 头像
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(36, 36));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 名称
        nameLabel = new JLabel();
        nameLabel.setFont(nameFont);
        nameLabel.setForeground(new Color(51, 51, 51));

        // 最后消息内容
        contentLabel = new JLabel();
        contentLabel.setFont(contentFont);
        contentLabel.setForeground(new Color(153, 153, 153));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // 时间
        timeLabel = new JLabel();
        timeLabel.setFont(timeFont);
        timeLabel.setForeground(new Color(153, 153, 153));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // 置顶图标
//        topIconLabel = new JLabel("📍");
        topIconLabel = new JLabel("📌");
        topIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
        topIconLabel.setForeground(new Color(255, 150, 0));
        topIconLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        topIconLabel.setVisible(false);

        // 静音图标
        muteIconLabel = new JLabel("🔕");
        muteIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
        muteIconLabel.setForeground(new Color(150, 150, 150));
        muteIconLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        muteIconLabel.setVisible(false);

        // 未读计数
        unreadLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制圆形背景
                g2.setColor(unreadColor);
                int diameter = 18; // 固定直径
                int x = (getWidth() - diameter) / 2;
                int y = (getHeight() - diameter) / 2;
                g2.fillOval(x, y, diameter, diameter);

                // 绘制文字
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getAscent();
                    int textX = (getWidth() - textWidth) / 2;
                    if (text.length() > 1) {
                        textX = textX - 1;
                    }
                    int textY = (getHeight() - textHeight) / 2 + fm.getAscent() - 2;
                    g2.drawString(text, textX, textY);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20); // 包含边距的固定大小
            }
        };
        unreadLabel.setFont(unreadFont);
        unreadLabel.setForeground(Color.WHITE);
        unreadLabel.setHorizontalAlignment(SwingConstants.CENTER);
        unreadLabel.setOpaque(false);
        unreadLabel.setVisible(false);
    }

    private void setupLayout() {
        // 左侧头像区域
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(avatarLabel, BorderLayout.CENTER);

        // 右侧内容区域
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);

        // 顶部区域：名称 + 图标 + 时间
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setOpaque(false);

        // 名称和图标容器
        JPanel nameIconsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        nameIconsPanel.setOpaque(false);
        nameIconsPanel.add(nameLabel);
        nameIconsPanel.add(topIconLabel);
        nameIconsPanel.add(muteIconLabel);

        topPanel.add(nameIconsPanel, BorderLayout.WEST);
        topPanel.add(timeLabel, BorderLayout.EAST);

        // 底部区域：消息内容 + 未读计数
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.add(contentLabel, BorderLayout.WEST);

        // 未读计数容器 - 使用边框布局确保垂直居中
        JPanel unreadPanel = new JPanel(new BorderLayout());
        unreadPanel.setOpaque(false);
        unreadPanel.setPreferredSize(new Dimension(30, 20)); // 固定宽度防止内容溢出
        unreadPanel.setMaximumSize(new Dimension(30, 20));
        unreadPanel.add(unreadLabel, BorderLayout.CENTER);
        bottomPanel.add(unreadPanel, BorderLayout.EAST);

        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(bottomPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends UserConversation> list,
                                                  UserConversation conversation, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        // 设置背景和前景色
        if (isSelected) {
            setBackground(new Color(220, 235, 252));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(74, 144, 226)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 10)
            ));
        } else {
            setBackground(index % 2 == 0 ?
                    new Color(250, 250, 252) :
                    new Color(245, 245, 247));
            setBorder(new EmptyBorder(8, 10, 8, 10));
        }


        // 设置头像
        String showName = conversation.getShowName();
        String avatar = conversation.getAvatar();

        String avatarText = conversation.getConversationType() == ConversationType.GROUP_VALUE ? "群聊" : showName;
        avatarLabel.setIcon(ImageUtil.getIconForName(avatarText, 32));

        // 设置名称
        nameLabel.setText(showName);

        // 设置最后消息内容
        String content = conversation.getLastMessageContent();
        if (content != null && content.length() > 10) {
            content = content.substring(0, 10) + "...";
        }
        contentLabel.setText(content != null ? content : "");

        // 设置时间
        if (conversation.getLastMessageTime() != null && conversation.getLastMessageTime() > 0) {
            timeLabel.setText(TimeUtil.formatTime(conversation.getLastMessageTime()));
        } else if (conversation.getCreateTime() != null && conversation.getCreateTime() > 0) {
            timeLabel.setText(TimeUtil.formatTime(conversation.getCreateTime()));
        } else {
            timeLabel.setText("");
        }

        // 设置置顶图标
        boolean isTop = conversation.getIsTop() != null && conversation.getIsTop();
        topIconLabel.setVisible(isTop);

        // 设置静音图标
        boolean isMute = conversation.getIsMute() != null && conversation.getIsMute();
        muteIconLabel.setVisible(isMute);

        // 设置未读计数 - 始终保持固定大小
        Integer unreadCount = conversation.getUnreadCount();
        if (unreadCount != null && unreadCount > 0) {
            String text = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
            unreadLabel.setText(text);
            unreadLabel.setVisible(true);
        } else {
            unreadLabel.setVisible(false);
        }

        return this;
    }


}