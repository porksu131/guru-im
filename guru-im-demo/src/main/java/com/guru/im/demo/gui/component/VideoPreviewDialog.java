package com.guru.im.demo.gui.component;// MainFrame.java

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class VideoPreviewDialog extends JFrame {
    private VideoPlayer videoPlayer;
    
    public VideoPreviewDialog(JFrame parent) {
        super("视频预览");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setIconImage(createAppIcon());
        setLocationRelativeTo(parent);
        initComponents();
    }
    
    private void initComponents() {
        // 创建主面板
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        // 创建视频播放器组件
        videoPlayer = new VideoPlayer();
        contentPane.add(videoPlayer, BorderLayout.CENTER);

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                videoPlayer.stop();
            }
        });
    }
    
    private Image createAppIcon() {
        // 创建一个简单的播放图标
        int size = 32;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制播放图标
        g2d.setColor(new Color(70, 130, 180));
        g2d.fillOval(2, 2, size-4, size-4);
        
        g2d.setColor(Color.WHITE);
        int[] xPoints = {12, 12, 22};
        int[] yPoints = {10, 22, 16};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.dispose();
        return icon;
    }

    public void showVideo(String videoName, String videoUrl) {
        setTitle("视频预览 - " + videoName);
        setVisible(true);
        videoPlayer.loadVideo(videoUrl);
    }
}