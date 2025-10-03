package com.guru.im.demo.gui.component;

import javax.swing.*;
import java.awt.*;

/**
 * 呼吸灯组件实现
 */
public class BreathingLightDot extends JPanel {
    private static final int MIN_SIZE = 4;
    private static final int MAX_SIZE = 8;
    private static final int CYCLE_TIME = 3000; // 3秒周期

    private Timer timer;
    private long startTime;
    private Color lightColor;
    private boolean isAnimating = false;

    public BreathingLightDot(Color color) {
        this.lightColor = color;
        setOpaque(false);
        setPreferredSize(new Dimension(12, 12));

        // 创建独立的动画定时器
        timer = new Timer(16, e -> {
            if (isAnimating && isVisible()) {
                repaint();
            }
        });
    }

    public void startBreathing() {
        if (!isAnimating) {
            startTime = System.currentTimeMillis();
            timer.start();
            isAnimating = true;
            setVisible(true);
        }
    }

    public void stopBreathing() {
        if (isAnimating) {
            timer.stop();
            isAnimating = false;
            setVisible(false);
        }
    }

    public void setLightColor(Color color) {
        this.lightColor = color;
        if (isAnimating) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isAnimating || !isVisible()) {
            return;
        }

        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 计算动画进度
        long currentTime = System.currentTimeMillis();
        double cyclePosition = ((currentTime - startTime) % CYCLE_TIME) / (double) CYCLE_TIME;
        double breathingFactor = (Math.sin(cyclePosition * 2 * Math.PI - Math.PI / 2) + 1) / 2;

        // 计算当前尺寸
        double currentSize = MIN_SIZE + (MAX_SIZE - MIN_SIZE) * breathingFactor;
        double x = (getWidth() - currentSize) / 2;
        double y = (getHeight() - currentSize) / 2;

        // 绘制发光效果
        drawGlowEffect(g2d, x, y, currentSize, breathingFactor);

        // 绘制主圆点
        int alpha = (int) (80 + 175 * breathingFactor); // 提高基础可见度
        Color currentColor = new Color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), alpha);
        g2d.setColor(currentColor);
        g2d.fillOval((int) x, (int) y, (int) currentSize, (int) currentSize);
    }

    private void drawGlowEffect(Graphics2D g2d, double x, double y, double size, double breathingFactor) {
        int glowLayers = 2; // 减少层数，避免过大
        for (int i = glowLayers; i > 0; i--) {
            double glowSize = size + i * 2; // 减小发光范围
            double glowX = (getWidth() - glowSize) / 2;
            double glowY = (getHeight() - glowSize) / 2;

            int glowAlpha = (int) (20 * breathingFactor / i);
            Color glowColor = new Color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), glowAlpha);
            g2d.setColor(glowColor);
            g2d.fillOval((int) glowX, (int) glowY, (int) glowSize, (int) glowSize);
        }
    }
}