package com.guru.im.demo.gui.layout.content;

import com.guru.im.demo.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class ContentPanel extends JPanel {
    private final LeftPanel leftPanel;
    private final RightPanel rightPanel;

    public ContentPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JSplitPane contentPanel = new JSplitPane();
        contentPanel.setDividerLocation(240);
        contentPanel.setDividerSize(1);
        contentPanel.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setBackground(new Color(240, 242, 245));

        this.leftPanel = new LeftPanel(mainFrame);
        this.rightPanel = new RightPanel(mainFrame);

        contentPanel.setLeftComponent(leftPanel);
        contentPanel.setRightComponent(rightPanel);

        add(contentPanel, BorderLayout.CENTER);
    }

    public LeftPanel getLeftPanel() {
        return leftPanel;
    }

    public RightPanel getRightPanel() {
        return rightPanel;
    }
}
