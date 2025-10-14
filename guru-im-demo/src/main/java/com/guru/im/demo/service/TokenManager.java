package com.guru.im.demo.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.UserInfo;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public class TokenManager {
    private static TokenManager instance;
    private Timer refreshTimer;
    private final UserInfo currentUser;
    private final MainFrame mainFrame;

    // 刷新缓冲时间（提前5分钟刷新）
    private static final long REFRESH_BUFFER_TIME = 5 * 60 * 1000;

    public TokenManager(MainFrame mainFrame) {
        this.currentUser = mainFrame.getCurrentUser();
        this.mainFrame = mainFrame;
        startRefreshTimer();
    }

    /**
     * 启动定时刷新任务
     */
    private void startRefreshTimer() {
        stopRefreshTimer(); // 先停止现有的

        if (mainFrame.isLogout()) return;

        if (currentUser == null) return;

        long refreshDelay = currentUser.getExpiresIn() - REFRESH_BUFFER_TIME;

        if (refreshDelay <= 0) {
            // 需要立即刷新
            refreshTokenImmediately();
        } else {
            refreshTimer = new Timer(true); // 守护线程
            refreshTimer.schedule(new RefreshTokenTask(), refreshDelay);
        }
    }

    /**
     * 定时刷新任务
     */
    private class RefreshTokenTask extends TimerTask {
        @Override
        public void run() {
            refreshTokenImmediately();
        }
    }

    /**
     * 立即刷新token
     */
    public void refreshTokenImmediately() {
        if (currentUser == null) return;
        if (mainFrame.isLogout()) return;

        SwingUtilities.invokeLater(() -> {
            try {
                // 调用刷新接口
                ResponseResult<UserInfo> response = ApiService.refreshToken(currentUser.getRefreshToken());
                if (response.getCode() != ResponseResult.SUCCESS) {
                    handleRefreshFailure();
                    return;
                }

                // 更新用户信息中的token
                currentUser.setAccessToken(response.getData().getAccessToken());
                currentUser.setRefreshToken(response.getData().getRefreshToken());

                // 需要更新imClient中的user信息
                mainFrame.getImClientManager().getUserInfo().setRefreshToken(response.getData().getRefreshToken());
                mainFrame.getImClientManager().getUserInfo().setAccessToken(response.getData().getAccessToken());

                // 重新启动定时器
                startRefreshTimer();

            } catch (Exception e) {
                System.err.println("Token刷新失败: " + e.getMessage());
                handleRefreshFailure();
            }
        });
    }

    /**
     * 处理刷新失败
     */
    private void handleRefreshFailure() {
        stopRefreshTimer();

        // 显示重新登录提示
        showLoginDialog();
    }

    /**
     * 停止定时器
     */
    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * 显示登录对话框
     */
    public void showLoginDialog() {
        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showConfirmDialog(
                    this.mainFrame,
                    "登录已过期，是否重新登录？",
                    "会话过期",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                // 触发重新登录
                if (this.mainFrame != null && this.mainFrame.isVisible()) {
                    this.mainFrame.logout();
                }
            } else {
                // 退出程序或返回登录界面
                System.exit(0);
            }
        });
    }
}