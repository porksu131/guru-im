package com.guru.im.demo.gui.group;

import com.guru.im.demo.model.Friend;
import com.guru.im.demo.model.Group;
import com.guru.im.demo.util.AvatarGenerator;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// 自定义好友列表渲染器，显示小红点
public class GroupListRenderer extends JPanel implements ListCellRenderer<Group> {
    private JLabel avatarLabel;
    private JLabel nameLabel;

    private final Font nameFont = new Font("Microsoft YaHei", Font.BOLD, 12);

    public GroupListRenderer() {
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
        avatarLabel.setPreferredSize(new Dimension(32, 32));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 名称
        nameLabel = new JLabel();
        nameLabel.setFont(nameFont);
        nameLabel.setForeground(new Color(51, 51, 51));

    }

    private void setupLayout() {
        // 左侧头像区域
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(avatarLabel, BorderLayout.CENTER);

        // 右侧名称区域
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(nameLabel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Group> list,
                                                  Group group, int index,
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
        String showName = group.getGroupName();
        avatarLabel.setIcon(ImageUtil.getIconForName("G", 32));

        // 设置名称
        nameLabel.setText(showName);

        return this;
    }
}
