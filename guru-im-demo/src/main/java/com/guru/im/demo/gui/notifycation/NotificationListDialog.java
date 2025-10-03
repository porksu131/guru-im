package com.guru.im.demo.gui.notifycation;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.model.Notification;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NotificationListDialog extends JDialog {
    private MainFrame mainFrame;
    private JPanel notificationListPanel;

    public NotificationListDialog(MainFrame mainFrame) {
        super(mainFrame, "通知", true);
        this.mainFrame = mainFrame;
        setSize(400, 450);

        initComponents();
        loadNotifications();
    }

    private void initComponents() {
        // 通知列表
        notificationListPanel = new JPanel();
        notificationListPanel.setLayout(new BoxLayout(notificationListPanel, BoxLayout.Y_AXIS));
        notificationListPanel.setBackground(new Color(249, 249, 249));

        JScrollPane scrollPane = new JScrollPane(notificationListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadNotifications() {
        notificationListPanel.removeAll();

        List<Notification> notifications = mainFrame.getDatabaseManager().getAllNotifications();

        if (notifications.isEmpty()) {
            JLabel emptyLabel = new JLabel("暂无通知", JLabel.CENTER);
            emptyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            notificationListPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            notificationListPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            for (Notification notification : notifications) {
                NotificationCard card = new NotificationCard(this, notification, mainFrame);
                notificationListPanel.add(card);
                notificationListPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            }
        }

        notificationListPanel.revalidate();
        notificationListPanel.repaint();

        mainFrame.updateNotificationUnreadCount();
    }


    // 添加刷新方法供详情页回调
    public void refreshNotifications() {
        loadNotifications();
    }
}
