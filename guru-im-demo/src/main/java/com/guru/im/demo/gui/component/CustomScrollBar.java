package com.guru.im.demo.gui.component;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class CustomScrollBar extends BasicScrollBarUI {
    
    private final int scrollBarWidth; // 滚动条宽度
    
    public CustomScrollBar() {
        this.scrollBarWidth = 4; // 默认宽度
    }
    
    public CustomScrollBar(int scrollBarWidth) {
        this.scrollBarWidth = scrollBarWidth;
    }
    
    @Override
    protected void configureScrollBarColors() {
        // 清除默认颜色配置
    }
    
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }
    
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }
    
    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }
    
    @Override
    public Dimension getPreferredSize(JComponent c) {
        // 控制滚动条的整体大小
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, scrollbar.getHeight());
        } else {
            return new Dimension(scrollbar.getWidth(), scrollBarWidth);
        }
    }
    
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制轨道 - 很窄
        g2.setColor(new Color(240, 240, 240));
        
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            // 垂直滚动条：轨道宽度与滚动条一致
            g2.fillRect(trackBounds.x, trackBounds.y, scrollBarWidth, trackBounds.height);
        } else {
            // 水平滚动条：轨道高度与滚动条一致
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, scrollBarWidth);
        }
        
        g2.dispose();
    }
    
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color thumbColor;
        if (isThumbRollover()) {
            thumbColor = new Color(100, 100, 100);
        } else {
            thumbColor = new Color(150, 150, 150);
        }
        
        g2.setColor(thumbColor);
        
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            // 垂直滚动条：thumb宽度略小于轨道，居中显示
            int thumbWidth = scrollBarWidth - 2; // 比轨道窄2像素
            int x = thumbBounds.x + (scrollBarWidth - thumbWidth) / 2;
            g2.fillRoundRect(x, thumbBounds.y, thumbWidth, thumbBounds.height, 6, 6);
        } else {
            // 水平滚动条：thumb高度略小于轨道，居中显示
            int thumbHeight = scrollBarWidth - 2;
            int y = thumbBounds.y + (scrollBarWidth - thumbHeight) / 2;
            g2.fillRoundRect(thumbBounds.x, y, thumbBounds.width, thumbHeight, 6, 6);
        }
        
        g2.dispose();
    }
    
    @Override
    protected Dimension getMinimumThumbSize() {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth - 2, 30);
        } else {
            return new Dimension(30, scrollBarWidth - 2);
        }
    }
}