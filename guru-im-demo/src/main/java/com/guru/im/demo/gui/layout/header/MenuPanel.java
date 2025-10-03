package com.guru.im.demo.gui.layout.header;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.MenuButton;
import com.guru.im.demo.gui.friend.FriendSearchDialog;
import com.guru.im.demo.gui.group.GroupChatCreateDialog;
import com.guru.im.demo.gui.notifycation.NotificationListDialog;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {
    private MainFrame mainFrame;
    private JPopupMenu mainMenuPopup;
    private MenuButton menuButton; // 改为自定义按钮
    private int newNotificationCount = 0; // 新通知数量

    public MenuPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        setBackground(new Color(240, 242, 245));
        setPreferredSize(new Dimension(300, 60));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);

        // 菜单按钮 - 使用带呼吸灯的自定义按钮
        menuButton = new MenuButton();
        menuButton.addActionListener(e -> showMainMenuPopup(menuButton));

        buttonPanel.add(menuButton, BorderLayout.CENTER);

        add(buttonPanel);
    }


    private void updateMenuButtonBadge() {
        // 更新菜单按钮的呼吸灯状态
        if (newNotificationCount > 0) {
            menuButton.showBreathingLight();
        } else {
            menuButton.hideBreathingLight();
        }
    }

    private void showMainMenuPopup(Component invoker) {
        // 每次显示时重新创建菜单，以便更新通知数量显示
        mainMenuPopup = new JPopupMenu();
        mainMenuPopup.setBorder(BorderFactory.createEmptyBorder());
        mainMenuPopup.setBorderPainted(false);

        // 菜单项列表
        String[] menuItems = {"系统通知", "添加好友", "创建群聊", "重载数据", "切换用户", "退出"};
        String[] icons = {"image/notice.svg", "image/add-friend.svg", "image/add-group.svg", "image/refresh.svg", "image/logout.svg", "image/exit.svg"};

        for (int i = 0; i < menuItems.length; i++) {
            JMenuItem menuItem = new JMenuItem(menuItems[i]);
            menuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

            // 设置图标（系统通知会根据未读数量动态添加徽标）
            Icon originalIcon = ImageUtil.createSVGIcon(icons[i], 12, Color.BLACK);
            if ("系统通知".equals(menuItems[i]) && newNotificationCount > 0) {
                menuItem.setIcon(createBadgedIcon(originalIcon));
            } else {
                menuItem.setIcon(originalIcon);
            }

            menuItem.setIconTextGap(10);
            menuItem.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            menuItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
            menuItem.setBorderPainted(false);
            menuItem.setFocusPainted(false);

            final String action = menuItems[i];
            menuItem.addActionListener(e -> handleMenuAction(action));

            mainMenuPopup.add(menuItem);

            // 添加分隔线（最后一个不添加）
            if (i < menuItems.length - 1) {
                mainMenuPopup.addSeparator();
            }
        }

        mainMenuPopup.show(invoker, -55, invoker.getHeight() + 5);
    }

    /**
     * 创建带徽标的图标
     */
    private Icon createBadgedIcon(Icon originalIcon) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                // 绘制原始图标
                originalIcon.paintIcon(c, g, x, y);

                // 在图标右上角绘制徽标
                if (newNotificationCount > 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 极小的红色圆点徽标
                    int badgeSize = 8;
                    int badgeX = x + getIconWidth() - badgeSize + 2; // 右上角
                    int badgeY = y - 2;

                    // 绘制红色圆形
                    g2.setColor(new Color(220, 53, 69));
                    g2.fillOval(badgeX, badgeY, badgeSize, badgeSize);

                    // 如果数量较少，可以显示数字（1-9）
                    if (newNotificationCount <= 9) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 6));
                        String countText = String.valueOf(newNotificationCount);
                        FontMetrics fm = g2.getFontMetrics();
                        int textWidth = fm.stringWidth(countText);
                        int textHeight = fm.getAscent();
                        g2.drawString(countText,
                                badgeX + (badgeSize - textWidth) / 2,
                                badgeY + (badgeSize + textHeight) / 2 - 1);
                    }

                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return originalIcon.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return originalIcon.getIconHeight();
            }
        };
    }

    private void handleMenuAction(String action) {
        switch (action) {
            case "系统通知":
                showNoticeDialog();
                break;
            case "添加好友":
                showAddFriendDialog();
                break;
            case "创建群聊":
                showCreateGroupDialog();
                break;
            case "重载数据":
                mainFrame.loadData();
                break;
            case "切换用户":
                mainFrame.logout();
                break;
            case "退出":
                int result = JOptionPane.showConfirmDialog(mainFrame, "确定要退出系统吗？", "确认退出", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
                break;
        }
    }

    // 更新通知数量的方法
    public void updateNotificationCount(int count) {
        if (count < 0) {
            count = 0; // 确保数量不为负
        }

        this.newNotificationCount = count;
        updateMenuButtonBadge();
    }

    // 增加通知数量
    public void incrementNotifications(int amount) {
        updateNotificationCount(newNotificationCount + amount);
    }

    // 减少通知数量
    public void decrementNotifications(int amount) {
        updateNotificationCount(Math.max(0, newNotificationCount - amount));
    }

    // 清空通知
    public void clearNotifications() {
        updateNotificationCount(0);
    }

    // 打开添加好友窗口
    public void showAddFriendDialog() {
        FriendSearchDialog friendSearchDialog = new FriendSearchDialog(mainFrame);
        friendSearchDialog.setLocationRelativeTo(mainFrame);
        friendSearchDialog.setVisible(true);
    }

    // 打开创建群聊窗口
    public void showCreateGroupDialog() {
        GroupChatCreateDialog groupChatCreateDialog = new GroupChatCreateDialog(mainFrame);
        groupChatCreateDialog.setLocationRelativeTo(mainFrame);
        groupChatCreateDialog.setVisible(true);
    }

    // 打开系统通知处理窗口
    public void showNoticeDialog() {
        NotificationListDialog notificationListDialog = new NotificationListDialog(mainFrame);
        notificationListDialog.setLocationRelativeTo(mainFrame);
        notificationListDialog.setVisible(true);

        this.mainFrame.updateNotificationUnreadCount();
    }
}