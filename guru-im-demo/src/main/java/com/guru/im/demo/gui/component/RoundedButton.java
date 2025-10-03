package com.guru.im.demo.gui.component;

import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {
    public RoundedButton() {
        super();
    }

    public RoundedButton(String text) {
        super(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
            g2d.setColor(getBackground().brighter());
        } else if (getModel().isRollover()) {
            g2d.setColor(getBackground().darker());
        } else {
            g2d.setColor(getBackground());
        }

        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

        // 绘制文本
        g2d.setColor(getForeground());
        g2d.setFont(getFont());
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getHeight();
        g2d.drawString(getText(),
                (getWidth() - textWidth) / 2,
                (getHeight() + textHeight) / 2 - fm.getDescent());

        g2d.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // 不绘制默认边框
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = 40;
        return size;
    }
}
