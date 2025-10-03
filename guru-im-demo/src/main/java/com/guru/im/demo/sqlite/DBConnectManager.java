package com.guru.im.demo.sqlite;

import com.guru.im.demo.util.AppUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectManager {
    private static final String DB_URL = "jdbc:sqlite:" + AppUtil.getAppDataDir() + "guru_im.db";
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.err.println("加载sqlite连接驱动时出错: " + e.getMessage());
        }
    }
    
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        enableWAL(connection);
        return connection;
    }
    
    public static void enableWAL(Connection connection) {
        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        } catch (SQLException e) {
            System.err.println("启用WAL出错: " + e.getMessage());
        }
    }
    
    public static void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库连接时出错: " + e.getMessage());
        }
    }
}