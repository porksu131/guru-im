package com.guru.im.demo.gui.layout.header;

import com.guru.im.demo.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private final UserPanel userInfoPanel;
    private final MenuPanel menuPanel;

    public HeaderPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 242, 245));
//        setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 223, 230)),
//                BorderFactory.createEmptyBorder(10, 5, 0, 15)
//        ));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 15));
        setPreferredSize(new Dimension(getWidth(), 100));
        setMinimumSize(new Dimension(getWidth(), 100));
        setMaximumSize(new Dimension(getWidth(), 100));

        // 左侧用户信息面板
        userInfoPanel = new UserPanel(mainFrame);
        add(userInfoPanel, BorderLayout.WEST);

        // 右侧功能区
        menuPanel = new MenuPanel(mainFrame);
        add(menuPanel, BorderLayout.EAST);
    }

    public UserPanel getUserInfoPanel() {
        return userInfoPanel;
    }

    public MenuPanel getFunctionPanel() {
        return menuPanel;
    }
}
