package com.guru.im.demo.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class ButtonUtil {
    public static JButton createIconButton(String iconPath, String tooltip) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                ImageUtil.setHighQualityRenderingHints(g2d);
                super.paintComponent(g);
            }
        };
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(32, 32));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 加载图标
        URL url = ButtonUtil.class.getClassLoader().getResource(iconPath);
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            Image scaledIcon = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledIcon));
        }

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(240, 240, 240));
                button.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
                button.setOpaque(false);
            }
        });

        return button;
    }

    public static JButton createSvgButton(String iconPath, String tooltip, int size, Color color) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                ImageUtil.setHighQualityRenderingHints(g2d);
                super.paintComponent(g);
            }
        };
        button.setToolTipText(tooltip);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 加载图标
        ImageIcon svgIcon = ImageUtil.createSVGIcon(iconPath, size, color);
        button.setIcon(svgIcon);
        return button;
    }
}
