package com.guru.im.demo.gui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class RoundedPasswordField extends JPasswordField {
    private int cornerRadius = 20;
    private Color borderColor = new Color(200, 200, 200);
    private Color focusBorderColor = new Color(46, 134, 222);
    private Color backgroundColor = Color.WHITE;
    private String placeholder;

    public RoundedPasswordField() {
        super();
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 5, 10));
        setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制背景
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // 调用父类绘制文本和光标
        super.paintComponent(g2);

        // 绘制占位符文本
        if (getPassword().length == 0 && placeholder != null && !hasFocus()) {
            g2.setColor(new Color(160, 160, 160));
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, getInsets().left, y);
        }

        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hasFocus()) {
            g2.setColor(focusBorderColor);
        } else {
            g2.setColor(borderColor);
        }

        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, cornerRadius, cornerRadius);
        g2.dispose();
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setFocusBorderColor(Color focusBorderColor) {
        this.focusBorderColor = focusBorderColor;
        repaint();
    }

    @Override
    public void setBackground(Color bg) {
        this.backgroundColor = bg;
        super.setBackground(bg);
        repaint();
    }

    // 重写setEchoChar以确保占位符正确显示
    @Override
    public void setEchoChar(char c) {
        super.setEchoChar(c);
        repaint();
    }
}