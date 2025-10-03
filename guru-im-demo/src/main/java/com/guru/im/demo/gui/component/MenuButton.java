package com.guru.im.demo.gui.component;

import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 带呼吸灯效果的菜单按钮
 */
public class MenuButton extends JLayeredPane {
    private JButton button;
    private BreathingLightDot breathingLight;
    private boolean lightVisible = false;

    public MenuButton() {
        setLayout(null);
        setPreferredSize(new Dimension(40, 40));
        setOpaque(false);

        // 创建基础按钮
        button = new JButton();
        button.setIcon(ImageUtil.createSVGIcon("image/menu.svg", 24, new Color(174, 173, 173)));
        button.setRolloverIcon(ImageUtil.createSVGIcon("image/menu.svg", 24, new Color(125, 124, 124).darker()));
        button.setPressedIcon(ImageUtil.createSVGIcon("image/menu.svg", 28, new Color(74, 144, 226)));
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        button.setToolTipText("菜单");
        button.setOpaque(true);
        button.setBackground(null);
        button.setForeground(Color.BLACK);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBounds(0, 0, 40, 40);

        // 创建呼吸灯（右上角位置）
        breathingLight = new BreathingLightDot(new Color(220, 53, 69)); // 红色呼吸灯
        breathingLight.setBounds(25, 5, 12, 12); // 右上角位置
        breathingLight.setOpaque(false);
        breathingLight.setVisible(false);

        // 添加到分层面板
        add(button, JLayeredPane.DEFAULT_LAYER);
        add(breathingLight, JLayeredPane.PALETTE_LAYER);

        // 添加按钮点击事件
        button.addActionListener(e -> fireActionPerformed());
    }

    public void showBreathingLight() {
        if (!lightVisible) {
            breathingLight.setVisible(true);
            breathingLight.startBreathing();
            lightVisible = true;
            repaint();
        }
    }

    public void hideBreathingLight() {
        if (lightVisible) {
            breathingLight.stopBreathing();
            breathingLight.setVisible(false);
            lightVisible = false;
            repaint();
        }
    }

    public boolean isBreathingLightVisible() {
        return lightVisible;
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    private void fireActionPerformed() {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "menuButtonClick");

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        // 动态调整按钮和呼吸灯大小
        if (button != null) {
            button.setBounds(0, 0, width, height);
        }
        if (breathingLight != null) {
            // 呼吸灯始终在右上角
            int lightSize = Math.min(width, height) / 3;
            int lightX = width - lightSize - 3;
            int lightY = 9;
            breathingLight.setBounds(lightX, lightY, lightSize, lightSize);
        }
    }
}