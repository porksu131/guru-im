package com.guru.im.demo.sqlite.dao;

import java.sql.*;

public abstract class BaseDao {
    protected static Connection connection;
    
    public static void setConnection(Connection conn) {
        connection = conn;
    }
    
    protected static void setNullableLong(PreparedStatement pstmt, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            pstmt.setLong(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.BIGINT);
        }
    }
    
    protected static void setNullableInteger(PreparedStatement pstmt, int parameterIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.INTEGER);
        }
    }
    
    protected static void setNullableString(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        if (value != null) {
            pstmt.setString(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.VARCHAR);
        }
    }
    
    // 抽象的建表方法，每个子类必须实现
    public abstract void createTable();
}