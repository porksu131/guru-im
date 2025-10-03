package com.guru.im.demo.gui.friend;

import com.guru.im.demo.model.Friend;
import com.guru.im.demo.util.AvatarGenerator;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// 自定义好友列表渲染器，显示小红点
public class FriendListRenderer extends JPanel implements ListCellRenderer<Friend> {
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel onlineStatusLabel;

    private final Color selectedColor = new Color(240, 242, 245);
    private final Color hoverColor = new Color(245, 245, 245);
    private final Color normalColor = Color.WHITE;
    private final Font nameFont = new Font("Microsoft YaHei", Font.BOLD, 12);
    private final Font onlineStatusFont = new Font("Microsoft YaHei", Font.PLAIN, 10);

    public FriendListRenderer() {
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

        // 在线状态
        onlineStatusLabel = new JLabel();
        onlineStatusLabel.setFont(onlineStatusFont);
        onlineStatusLabel.setForeground(new Color(153, 153, 153));
        onlineStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

    }

    private void setupLayout() {
        // 左侧头像区域
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(avatarLabel, BorderLayout.CENTER);

        // 右侧内容区域
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);

        // 顶部区域：名称
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setOpaque(false);

        // 名称
        JPanel nameIconsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        nameIconsPanel.setOpaque(false);
        nameIconsPanel.add(nameLabel);


        topPanel.add(nameIconsPanel, BorderLayout.WEST);

        // 底部区域：在线状态
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.add(onlineStatusLabel, BorderLayout.WEST);

        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(bottomPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Friend> list,
                                                  Friend friend, int index,
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
        String showName = friend.getFriendName();
        String avatar = null;

        ImageIcon avatarIcon = AvatarGenerator.createCircleAvatar(showName, avatar, 32);
        avatarLabel.setIcon(ImageUtil.getIconForName(showName, 32));

        // 设置名称
        nameLabel.setText(showName);

        // 设置在线状态
        if (friend.isOnline()) {
            onlineStatusLabel.setText("在线");
            onlineStatusLabel.setForeground(new Color(88, 209, 134));
        } else {
            onlineStatusLabel.setText("离线");
            onlineStatusLabel.setForeground(new Color(150, 150, 150));
        }

        return this;
    }
}
