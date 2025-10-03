package com.guru.im.demo.gui.layout.content;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.conversation.ConversationListPanel;
import com.guru.im.demo.gui.friend.FriendListPanel;
import com.guru.im.demo.gui.group.GroupListPanel;

import javax.swing.*;
import java.awt.*;

public class LeftPanel extends JPanel {
    private final MainFrame mainFrame;
    private ConversationListPanel conversationListPanel;
    private FriendListPanel friendListPanel;
    private GroupListPanel groupListPanel;
    private JButton conversationButton;
    private JButton friendButton;
    private JButton groupButton;

    public LeftPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 创建切换按钮面板
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        switchPanel.setBackground(new Color(248, 249, 250));
        switchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        conversationButton = createTabButton("会话", true);
        friendButton = createTabButton("好友", false);
        groupButton = createTabButton("群组", false);

        switchPanel.add(conversationButton);
        switchPanel.add(friendButton);
        switchPanel.add(groupButton);

        // 创建卡片布局用于切换会话和好友列表
        JPanel cardPanel = new JPanel(new CardLayout());
        conversationListPanel = new ConversationListPanel(mainFrame);
        friendListPanel = new FriendListPanel(mainFrame);
        groupListPanel = new GroupListPanel(mainFrame);

        cardPanel.add(conversationListPanel, "conversation");
        cardPanel.add(friendListPanel, "friend");
        cardPanel.add(groupListPanel, "group");

        add(switchPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JButton createTabButton(String text, boolean selected) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(80, 40));
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (selected) {
            button.setForeground(new Color(0, 150, 255));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 150, 255)),
                    BorderFactory.createEmptyBorder(5, 15, 3, 15)
            ));
        } else {
            button.setForeground(new Color(102, 102, 102));
        }

        button.addActionListener(e -> switchTab(text));
        return button;
    }

    private void switchTab(String tabName) {
        CardLayout cl = (CardLayout) ((JPanel) conversationListPanel.getParent()).getLayout();

        if ("会话".equals(tabName)) {
            cl.show(conversationListPanel.getParent(), "conversation");
            toggleButtons(conversationButton);
        } else if ("好友".equals(tabName)) {
            cl.show(friendListPanel.getParent(), "friend");
            toggleButtons(friendButton);
        } else {
            cl.show(groupListPanel.getParent(), "group");
            toggleButtons(groupButton);
        }
    }

    public ConversationListPanel getConversationListPanel() {
        return conversationListPanel;
    }

    public FriendListPanel getFriendListPanel() {
        return friendListPanel;
    }

    public GroupListPanel getGroupListPanel() {
        return groupListPanel;
    }

    public void toggleButtons(JButton selectedButton) {
        setButtonSelected(selectedButton);
        if (selectedButton.getText().equals("会话")) {
            setButtonNoSelected(friendButton);
            setButtonNoSelected(groupButton);
        } else if (selectedButton.getText().equals("好友")) {
            setButtonNoSelected(conversationButton);
            setButtonNoSelected(groupButton);
        } else {
            setButtonNoSelected(conversationButton);
            setButtonNoSelected(friendButton);
        }
    }

    private void setButtonSelected(JButton selectedButton) {
        selectedButton.setForeground(new Color(0, 150, 255));
        selectedButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 150, 255)),
                BorderFactory.createEmptyBorder(5, 15, 3, 15)
        ));
    }

    private void setButtonNoSelected(JButton noSelectedButton) {
        noSelectedButton.setForeground(new Color(102, 102, 102));
        noSelectedButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    }
}
