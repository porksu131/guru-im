package com.guru.im.demo.gui.group;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.RoundedTextArea;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.model.Friend;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.demo.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GroupChatCreateDialog extends JDialog {
    private RoundedTextField groupNameField;
    private RoundedTextArea groupDescArea;
    private JList<Friend> friendsList;
    private DefaultListModel<Friend> listModel;
    private JButton createButton;
    private JButton cancelButton;
    private JLabel cardTitle;

    // 存储选择状态
    private List<Boolean> selectionStates;
    private List<Friend> friends;
    private UserInfo currentUser;
    private MainFrame mainFrame;

    public GroupChatCreateDialog(MainFrame mainFrame) {
        super(mainFrame, "创建群聊", true);
        this.mainFrame = mainFrame;
        this.currentUser = mainFrame.getCurrentUser();
        this.friends = mainFrame.getFriendListPanel().getFriends();
        this.selectionStates = new ArrayList<>(this.friends.size());
        for (int i = 0; i < this.friends.size(); i++) {
            selectionStates.add(false);
        }
        initializeUI();
        setupLayout();
        addEventListeners();
    }

    private void initializeUI() {
        // 设置对话框属性
        setSize(450, 550);
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private void setupLayout() {
        // 主容器
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(mainPanel);

        // 内容面板
        JPanel contentPanel = createContentPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(245, 245, 245));

        // 群聊信息卡片
        JPanel infoCard = createInfoCard();
        contentPanel.add(infoCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 成员选择卡片
        JPanel membersCard = createMembersCard();
        contentPanel.add(membersCard);

        return contentPanel;
    }

    private JPanel createInfoCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 卡片标题
        JLabel cardTitle = new JLabel("群聊信息");
        cardTitle.setFont(new Font("微软雅黑", Font.BOLD, 14));
        cardTitle.setForeground(new Color(80, 80, 80));
        card.add(cardTitle, BorderLayout.NORTH);

        // 表单内容
        JPanel formPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        formPanel.setBackground(Color.WHITE);

        // 群名称
        JPanel namePanel = new JPanel(new BorderLayout(10, 5));
        namePanel.setBackground(Color.WHITE);
        JLabel nameLabel = new JLabel("群名称");
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        groupNameField = new RoundedTextField();
        groupNameField.setPlaceholder("请输入群名称");
        groupNameField.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        namePanel.add(nameLabel, BorderLayout.WEST);
        namePanel.add(groupNameField, BorderLayout.CENTER);

        // 群描述
        JPanel descPanel = new JPanel(new BorderLayout(10, 5));
        descPanel.setBackground(Color.WHITE);
        JLabel descLabel = new JLabel("群描述");
        descLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        groupDescArea = new RoundedTextArea();
        groupDescArea.setPlaceholder("请输入群描述");
        groupDescArea.setRows(3);
        groupDescArea.setColumns(20);
        groupDescArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        groupDescArea.setLineWrap(true);
        groupDescArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(groupDescArea);
        descScrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        descScrollPane.setBorder(BorderFactory.createEmptyBorder());
        descPanel.add(descLabel, BorderLayout.WEST);
        descPanel.add(groupDescArea, BorderLayout.CENTER);

        formPanel.add(namePanel);
        formPanel.add(descPanel);

        card.add(formPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMembersCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 卡片标题
        cardTitle = new JLabel("已选择成员 (" + getSelectedFriends().size() + ")");
        cardTitle.setFont(new Font("微软雅黑", Font.BOLD, 14));
        cardTitle.setForeground(new Color(80, 80, 80));
        card.add(cardTitle, BorderLayout.NORTH);

        // 创建JList
        listModel = new DefaultListModel<>();
        for (Friend friend : friends) {
            listModel.addElement(friend);
        }

        friendsList = new JList<>(listModel);
        friendsList.setCellRenderer(new FriendListCellRenderer());
        friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendsList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        friendsList.setFixedCellHeight(50);
        friendsList.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(friendsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    // 自定义渲染器
    private class FriendListCellRenderer extends JPanel implements ListCellRenderer<Friend> {
        private JLabel avatarLabel;
        private JLabel nameLabel;
        private JLabel statusLabel;
        private JCheckBox checkBox;

        public FriendListCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBorder(new EmptyBorder(5, 15, 5, 15));

            // 左侧：头像
            avatarLabel = new JLabel();
            avatarLabel.setPreferredSize(new Dimension(40, 40));

            // 中间：名称和状态
            JPanel infoPanel = new JPanel(new BorderLayout(0, 2));
            infoPanel.setOpaque(false);

            nameLabel = new JLabel();
            nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

            statusLabel = new JLabel();
            statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(statusLabel, BorderLayout.SOUTH);

            // 右侧：复选框 - 修复显示问题
            checkBox = new JCheckBox();
            checkBox.setOpaque(false);
            checkBox.setFocusPainted(false);
            checkBox.setBackground(Color.WHITE);
            // 确保复选框有足够空间
            checkBox.setPreferredSize(new Dimension(20, 20));
            checkBox.setMargin(new Insets(0, 0, 0, 0));

            add(avatarLabel, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
            add(checkBox, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Friend> list, Friend friend,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            // 设置名称
            nameLabel.setText(friend.getFriendName());

            // 设置在线状态
            if (friend.isOnline()) {
                statusLabel.setText("在线");
                statusLabel.setForeground(new Color(0, 180, 0));
            } else {
                statusLabel.setText("离线");
                statusLabel.setForeground(Color.GRAY);
            }

            // 设置头像
            ImageIcon originalIcon = generateAvatar(friend.getFriendName(), friend.isOnline());
            avatarLabel.setIcon(ImageUtil.getIconForName(friend.getFriendName(), 32));

            // 设置复选框状态 - 使用独立的选择状态列表
            checkBox.setSelected(selectionStates.get(index));

            // 设置背景色
            if (isSelected) {
                setBackground(new Color(220, 240, 255));
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : new Color(250, 250, 250));
            }

            return this;
        }

        // 生成头像
        private ImageIcon generateAvatar(String name, boolean isOnline) {
            int size = 32;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            ImageUtil.setHighQualityRenderingHints(g2d);

            // 绘制圆形背景
            Color bgColor = isOnline ? new Color(70, 130, 230) : new Color(150, 150, 150);
            g2d.setColor(bgColor);
            g2d.fillOval(0, 0, size, size);

            // 绘制首字母
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 16));
            String initial = name.substring(0, 1);
            FontMetrics fm = g2d.getFontMetrics();
            int x = (size - fm.stringWidth(initial)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(initial, x, y);

            g2d.dispose();
            return new ImageIcon(image);
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));

        // 取消按钮
        cancelButton = new JButton("取消");
        cancelButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 创建按钮
        createButton = new JButton("创建");
        createButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        createButton.setBackground(new Color(0, 150, 136));
        createButton.setForeground(Color.WHITE);
        createButton.setPreferredSize(new Dimension(80, 30));
        createButton.setFocusPainted(false);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        return buttonPanel;
    }

    private void addEventListeners() {
        // 取消按钮事件
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // 创建按钮事件
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroup();
            }
        });

        // 使用鼠标监听器处理复选框点击
        friendsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = friendsList.locationToIndex(e.getPoint());
                if (index >= 0 && index < selectionStates.size()) {
                    // 切换选择状态
                    selectionStates.set(index, !selectionStates.get(index));
                    // 更新UI
                    updateUIWhenSelectChanged();
                    // 重绘列表
                    friendsList.repaint();
                }
            }
        });
    }

    private void updateUIWhenSelectChanged() {
        List<Friend> selectedFriends = getSelectedFriends();
        cardTitle.setText("已选择成员 (" + selectedFriends.size() + ")");
        if (StringUtils.isBlank(groupNameField.getText()) || groupNameField.getText().contains(",")) {
            List<String> selectedFriendsNames = selectedFriends.stream().map(Friend::getFriendName).toList();
            groupNameField.setText(currentUser.getUserName() + "," + String.join(",", selectedFriendsNames));
        }
    }

    private void createGroup() {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入群名称", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 获取选中的成员
        List<Friend> selectedFriends = getSelectedFriends();

        if (selectedFriends.size() < 2) {
            JOptionPane.showMessageDialog(this, "请至少选择两名成员", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Long> memberIds = selectedFriends.stream().map(Friend::getFriendId).toList();

        try {
            ResponseResult<Void> addResult = ApiService.sendGroupCreate(currentUser.getUid(), groupName,
                    groupDescArea.getText().trim(), "", memberIds, currentUser.getAccessToken());
            if (ResponseResult.isSuccess(addResult)) {
                JOptionPane.showMessageDialog(this, "群聊" + groupName + "已创建!", "成功", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.getGroupListPanel().loadGroupList(); // 重新加载群聊列表
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, addResult.getMsg(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "群聊创建失败:" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
        }

        dispose();
    }

    private List<Friend> getSelectedFriends() {
        // 获取选中的成员
        List<Friend> selectedFriends = new ArrayList<>();
        for (int i = 0; i < selectionStates.size(); i++) {
            if (selectionStates.get(i)) {
                selectedFriends.add(friends.get(i));
            }
        }
        return selectedFriends;
    }
}