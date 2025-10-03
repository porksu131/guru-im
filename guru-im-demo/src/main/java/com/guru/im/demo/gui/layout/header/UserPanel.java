package com.guru.im.demo.gui.layout.header;

import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserPanel extends JPanel {
    private UserInfo currentUser;
    private int onlineStatus = OnlineStatus.OFFLINE;
    private JPanel avatarPanel;

    public UserPanel(MainFrame mainFrame) {
        this.currentUser = mainFrame.getCurrentUser();
        setBackground(new Color(240, 242, 245));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // 创建圆形头像
        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制头像背景
                g2.setColor(new Color(220, 223, 230));
                g2.fillOval(5, 5, 60, 60);

                // 绘制用户头像图片
                g2.setColor(new Color(52, 152, 219));
                g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
                g2.drawString(mainFrame.getCurrentUser().getUserName().substring(0, 1), 23, 43);

                // 绘制在线状态指示器
                if (onlineStatus == OnlineStatus.ONLINE) {
                    g2.setColor(new Color(46, 204, 113)); // 在线状态颜色
                } else if (onlineStatus == OnlineStatus.OFFLINE) {
                    g2.setColor(new Color(158, 158, 158)); // 离线状态颜色
                } else {
                    g2.setColor(new Color(255, 0, 0)); // 连接失败
                }
                g2.fillOval(50, 50, 15, 15);

                // 添加白色边框
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(50, 50, 15, 15);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(70, 70));
        avatarPanel.setBackground(null);

        // 创建用户信息文本面板 - 使用GridBagLayout确保正确对齐
        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setBackground(new Color(240, 242, 245));
        textPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 用户名
        JLabel nameLabel = new JLabel(currentUser.getUserName());
        nameLabel.setFont(new Font("PingFang SC", Font.BOLD, 18));
        nameLabel.setForeground(new Color(52, 73, 94));
        textPanel.add(nameLabel, gbc);

        // 用户ID区域
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        idPanel.setBackground(new Color(240, 242, 245));

        JLabel idLabel = new JLabel("ID: " + currentUser.getUid());
        idLabel.setFont(new Font("PingFang SC", Font.PLAIN, 14));
        idLabel.setForeground(new Color(127, 140, 141));
        idLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // 复制按钮
        JButton copyButton = new JButton();
        copyButton.setIcon(ImageUtil.createSVGIcon("image/copy.svg",16, new Color(190, 190, 190)));
        copyButton.setRolloverIcon(ImageUtil.createSVGIcon("image/copy.svg", 16, new Color(241, 214, 159)));
        copyButton.setPressedIcon(ImageUtil.createSVGIcon("image/copy.svg", 16, new Color(69, 177, 231)));
        copyButton.setPreferredSize(new Dimension(20, 20));
        copyButton.setBorder(BorderFactory.createEmptyBorder());
        copyButton.setContentAreaFilled(false);
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("复制");
        copyButton.setFocusable(Boolean.FALSE);
        copyButton.setFocusPainted(false);
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 复制ID到剪贴板的逻辑
                StringSelection stringSelection = new StringSelection(String.valueOf(currentUser.getUid()));
                java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });

        idPanel.add(idLabel);
        idPanel.add(copyButton);
        textPanel.add(idPanel, gbc);

        // 用户签名
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel signatureLabel = new JLabel("生活不止眼前的苟且，还有诗和远方的田野");
        signatureLabel.setFont(new Font("PingFang SC", Font.PLAIN, 14));
        signatureLabel.setForeground(new Color(149, 165, 166));
        textPanel.add(signatureLabel, gbc);

        add(avatarPanel);
        add(textPanel);
    }

    // 更新用户在线状态
    public void updateOnlineStatus(int onlineStatus) {
        this.onlineStatus = onlineStatus;
        if (avatarPanel != null) {
            avatarPanel.repaint(); // 重绘头像面板以更新状态指示器
        }
    }
}
