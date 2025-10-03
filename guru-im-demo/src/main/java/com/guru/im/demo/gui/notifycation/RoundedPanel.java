package com.guru.im.demo.gui.notifycation;

import javax.swing.*;
import java.awt.*;

// 圆角面板（保持不变）
public class RoundedPanel extends JPanel {
    private int cornerRadius;
    
    public RoundedPanel(int cornerRadius) {
        super();
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
    }
}

