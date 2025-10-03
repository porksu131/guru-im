package com.guru.im.demo.gui.friend;

import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.demo.model.Friend;

import javax.swing.*;
import java.awt.*;

// 自定义好友列表渲染器，显示小红点
public class FriendListRendererOld extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(237, 255, 237, 255));
        panel.setBorder(BorderFactory.createEmptyBorder());

        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setOpaque(false);
        label.setForeground(Color.BLACK);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));

        if (value instanceof Friend) {
            Friend friend = (Friend) value;

            label.setText(friend.getFriendName());
            //label.setText(getOnlineStatusText(friend));
            label.setFocusable(false);

            if (isSelected) {
                panel.setBackground(new Color(8, 151, 136, 197));
                label.setForeground(Color.WHITE);
            }

            panel.add(label, BorderLayout.WEST);

            JLabel onlineStatusDot = getOnlineStatusDot(friend.getOnlineStatus());
            panel.add(onlineStatusDot, BorderLayout.CENTER);

            // 显示未读数量
            if (friend.getUnReadCount() > 0) {
                JLabel dot = new JLabel();
                dot.setOpaque(false);
                dot.setForeground(Color.RED);
                dot.setPreferredSize(new Dimension(36, 12));
                dot.setText(friend.getUnReadCount() + "");
                panel.add(dot, BorderLayout.EAST);
            }
        }
        return panel;
    }

    private String getOnlineStatusText(Friend friend) {
        String onlineStatusText = "";
        if (friend.getOnlineStatus() != null && friend.getOnlineStatus() == OnlineStatus.ONLINE) {
            onlineStatusText = "<html><span>" + friend.getFriendName() + "</span>    [<span style='color:green'>在线</span>]</html>";
        } else {
            onlineStatusText = "<html><span>" + friend.getFriendName() + "</span>    [<span style='color:gray'>离线</span>]</html>";
        }
        return onlineStatusText;
    }

    private JLabel getOnlineStatusDot(Integer onlineStatus) {
        Color color = onlineStatus != null && onlineStatus == OnlineStatus.ONLINE ? Color.GREEN : Color.LIGHT_GRAY;
        JLabel dot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillOval(12, 12, 8, 8);
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        return dot;
    }
}
