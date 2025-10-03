package com.guru.im.demo.gui.friend;

import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.model.Friend;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.protocol.model.PresenceNotify;
import com.guru.im.protocol.model.UserPresence;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FriendListPanel extends JPanel {
    private final MainFrame mainFrame;
    private final UserInfo currentUser;
    private DefaultListModel<Friend> friendListModel;
    private JList<Friend> friendList;
    private JScrollPane scrollPane;
    private List<Friend> friends = new ArrayList<>();

    public FriendListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.currentUser = mainFrame.getCurrentUser();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initComponents();
        setUI();
    }

    private void setUI() {
        setLayout(new BorderLayout());
        // 创建搜索框
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(new Color(248, 249, 250));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        RoundedTextField searchField = new RoundedTextField();
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.setPlaceholder("输入回车搜索");

        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initComponents() {
        this.friendListModel = new DefaultListModel<>();
        this.friendList = new JList<>(friendListModel);
        this.friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.friendList.setCellRenderer(new FriendListRenderer());
        this.friendList.setFixedCellHeight(58);
        this.friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击打开会话窗口
                    mainFrame.getChatPanelWrapper().showChatPanel(friendList.getSelectedValue().getUserConversation());
                }
            }
        });

        this.friendList.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        this.friendList.setBackground(new Color(248, 249, 250));

        this.scrollPane = new JScrollPane(friendList);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void receiveFriendNotify(PresenceNotify friendOnlineNotify) {
        long friendId = friendOnlineNotify.getUserId();
        // 暂时这么处理，要么在线，要么离线
        int onlineStatus = UserPresence.ONLINE == friendOnlineNotify.getStatus() ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE;
        updateOnlineStatus(friendId, onlineStatus);
        sortFriendList();
    }

    public void updateOnlineStatus(long friendId, int onlineStatus) {
        for (int i = 0; i < friendListModel.getSize(); i++) {
            Friend f = friendListModel.getElementAt(i);
            if (f.getFriendId() == friendId) {
                f.setOnlineStatus(onlineStatus);
                repaint();
            }
        }
    }

    public void updateAllOfflineStatus() {
        for (int i = 0; i < friendListModel.getSize(); i++) {
            Friend f = friendListModel.getElementAt(i);
            f.setOnlineStatus(OnlineStatus.OFFLINE);
        }
        repaint();
    }


    public boolean existFriend(long friendId) {
        for (int i = 0; i < friendListModel.getSize(); i++) {
            Friend f = friendListModel.getElementAt(i);
            if (f.getFriendId() == friendId) {
                return true;
            }
        }
        return false;
    }

    public void loadFriendList() {
        ResponseResult<List<Friend>> responseResult = ApiService.getFriendList(currentUser.getUid(), currentUser.getAccessToken());
        if (!ResponseResult.isSuccess(responseResult)) {
            System.err.println("loadGroupList error: " + responseResult.getMsg());
            return;
        }
        List<Friend> allFriends = responseResult.getData();
        if (CollectionUtils.isEmpty(allFriends)) {
            return;
        }
        friends.clear();
        friends.addAll(allFriends);
        Friend me = allFriends.stream()
                .filter(friend -> friend.getFriendId() == currentUser.getUid())
                .findFirst().orElse(null);
        if (me != null) {
            me.setFriendName("我");
            allFriends.remove(me);
            allFriends.add(0, me);
        }
        if (friendListModel.isEmpty()) {
            friendListModel.addAll(allFriends);
        } else {
            List<Friend> newFriends = findNewFriends(allFriends);
            friendListModel.addAll(newFriends);
        }

        sortFriendList();

        this.friendList.repaint();
    }

    private List<Friend> findNewFriends(List<Friend> friends) {
        List<Friend> newFriends = new ArrayList<>();
        for (Friend friend : friends) {
            if (!existFriend(friend.getFriendId())) {
                newFriends.add(friend);
            }
        }
        return newFriends;
    }


    public void sortFriendList() {
        // 1. 提取模型中的数据到临时列表
        List<Friend> friends = new ArrayList<>();
        for (int i = 0; i < friendListModel.getSize(); i++) {
            friends.add(friendListModel.getElementAt(i));
        }

        // 2. 使用自定义比较器排序
        friends.sort(new Comparator<Friend>() {
            @Override
            public int compare(Friend f1, Friend f2) {
                boolean isMe1 = "我".equals(f1.getFriendName());
                boolean isMe2 = "我".equals(f2.getFriendName());

                if (isMe1 && isMe2) return 0;            // 两个都是"我"
                if (isMe1) return -1;                    // f1是"我"置顶
                if (isMe2) return 1;                     // f2是"我"置顶

                // 在线状态优先：在线排在前（true的"大于"false，返回负值使在线靠前）
                if (f1.isOnline() && !f2.isOnline()) {
                    return -1;  // f1在线，f2离线 -> f1排在f2前
                } else if (!f1.isOnline() && f2.isOnline()) {
                    return 1;   // f1离线，f2在线 -> f1排在f2后
                } else {
                    // 状态相同则按名称排序
                    return f1.getFriendName().compareTo(f2.getFriendName());
                }
            }
        });

        // 3. 清空并重新填充模型
        friendListModel.clear();
        for (Friend friend : friends) {
            friendListModel.addElement(friend);
        }
    }

    public List<Friend> getFriends() {
        return friends;
    }
}
