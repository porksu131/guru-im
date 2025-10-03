package com.guru.im.demo.gui.group;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.model.Group;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GroupListPanel extends JPanel {
    private final MainFrame mainFrame;
    private final UserInfo currentUser;
    private DefaultListModel<Group> groupListModel;
    private JList<Group> groupList;
    private JScrollPane scrollPane;
    private List<Group> groups = new ArrayList<>();

    public GroupListPanel(MainFrame mainFrame) {
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
        this.groupListModel = new DefaultListModel<>();
        this.groupList = new JList<>(groupListModel);
        this.groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.groupList.setCellRenderer(new GroupListRenderer());
        this.groupList.setFixedCellHeight(58);
//        this.groupList.addListSelectionListener((event) -> {
//            mainFrame.getChatPanelWrapper().showChatPanel(groupList.getSelectedValue().getUserConversation()); // todo
//        });
        this.groupList.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        this.groupList.setBackground(new Color(248, 249, 250));

        this.scrollPane = new JScrollPane(groupList);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }


    public void loadGroupList() {
        ResponseResult<List<Group>> responseResult = ApiService.getGroupList(currentUser.getUid(), currentUser.getAccessToken());
        if (!ResponseResult.isSuccess(responseResult)) {
            System.err.println("loadGroupList error: " + responseResult.getMsg());
            return;
        }
        List<Group> allGroups = responseResult.getData();
        if (CollectionUtils.isNotEmpty(allGroups)) {
            groups.clear();
            groups.addAll(allGroups);
            if (groupListModel.isEmpty()) {
                groupListModel.addAll(allGroups);
            } else {
                List<Group> newFriends = findNewGroups(allGroups);
                groupListModel.addAll(newFriends);
            }
            this.groupList.repaint();
        }
    }


    private List<Group> findNewGroups(List<Group> groups) {
        List<Group> newGroups = new ArrayList<>();
        for (Group group : groups) {
            if (!existGroup(group.getId())) {
                newGroups.add(group);
            }
        }
        return newGroups;
    }

    private boolean existGroup(Long id) {
        if (groupListModel.isEmpty()) {
            return false;
        }
        for (int i = 0; i < groupListModel.getSize(); i++) {
            if (groupListModel.get(i).getId().equals(id)) {
                return true;
            }
        }
        return false;
    }


    public List<Group> getGroups() {
        return groups;
    }
}
