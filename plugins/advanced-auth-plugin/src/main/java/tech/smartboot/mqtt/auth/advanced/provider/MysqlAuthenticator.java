/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经 smartboot 组织特别许可，需遵循 AGPL-3.0 开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced.provider;

import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.config.MysqlConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 认证器
 * 从 MySQL 数据库查询用户凭证进行认证，适用于传统企业应用和已有数据库系统
 * <p>
 * 配置选项：
 * - url: JDBC 连接 URL（必填，如 jdbc:mysql://localhost:3306/mqtt）
 * - username: 数据库用户名（必填）
 * - password: 数据库密码（必填）
 * - driverClass: JDBC 驱动类名（默认 com.mysql.cj.jdbc.Driver）
 * - tableName: 用户表名（默认 mqtt_users）
 * - usernameColumn: 用户名字段名（默认 username）
 * - passwordColumn: 密码字段名（默认 password）
 * - whereClause: 额外的 WHERE 条件（可选，如 " AND status=1"）
 * - connectionTimeout: 连接超时时间（默认 3000ms）
 * - maxConnections: 最大连接数（默认 5）
 * <p>
 * 数据表结构示例：
 * CREATE TABLE mqtt_users (
 * id INT PRIMARY KEY AUTO_INCREMENT,
 * username VARCHAR(64) NOT NULL UNIQUE,
 * password VARCHAR(256) NOT NULL,
 * created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class MysqlAuthenticator extends AbstractAuthenticator {

    private String url;
    private String dbUsername;
    private String dbPassword;
    private String driverClass = "com.mysql.cj.jdbc.Driver";
    private String tableName = "mqtt_users";
    private String usernameColumn = "username";
    private String passwordColumn = "password";
    private String whereClause = "";
    private int connectionTimeout = 3000;
    private int maxConnections = 5;

    // 简单的连接池
    private final ConcurrentHashMap<String, Connection> connectionPool = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    private MysqlConfig config;

    public MysqlAuthenticator(MysqlConfig mysql) {
        this.config = mysql;
    }

    @Override
    public void initialize() {
        // 加载 JDBC 驱动
        loadDriver();

        // 初始化连接池
        initializeConnectionPool();
    }


    /**
     * 加载 JDBC 驱动
     */
    private void loadDriver() {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load JDBC driver: " + driverClass, e);
        }
    }

    /**
     * 初始化连接池
     */
    private void initializeConnectionPool() {
        // 预创建连接
        for (int i = 0; i < Math.min(maxConnections, 3); i++) {
            createConnection();
        }
    }

    /**
     * 创建数据库连接
     */
    private Connection createConnection() {
        try {
            Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
            conn.setNetworkTimeout(null, connectionTimeout);
            connectionPool.put(conn.toString(), conn);
            return conn;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database connection", e);
        }
    }

    /**
     * 获取数据库连接
     */
    private synchronized Connection getConnection() {
        // 尝试从池中获取连接
        for (Connection conn : connectionPool.values()) {
            try {
                if (conn != null && !conn.isClosed()) {
                    return conn;
                }
            } catch (Exception e) {
                // 连接已失效，移除
                connectionPool.remove(conn.toString());
            }
        }

        // 创建新连接
        return createConnection();
    }

    @Override
    public void destroy() {
        initialized = false;
        // 关闭所有连接
        for (Connection conn : connectionPool.values()) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        connectionPool.clear();
    }

    @Override
    public CompletableFuture<AuthResult> authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] passwordBytes = getPassword(message);

        if (username == null || username.isEmpty() || passwordBytes == null || passwordBytes.length == 0) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        String password = new String(passwordBytes, StandardCharsets.UTF_8);

        if (!initialized) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            // 构建查询 SQL
            String sql = "SELECT " + passwordColumn + " FROM " + tableName +
                    " WHERE " + usernameColumn + " = ?" + whereClause;

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                String expectedPassword = rs.getString(passwordColumn);

                if (verifyPassword(username, passwordBytes, expectedPassword)) {
                    return CompletableFuture.completedFuture(AuthResult.SUCCESS);
                } else {
                    return CompletableFuture.completedFuture(AuthResult.FAILURE);
                }
            } else {
                // 用户不存在，让下一个认证器处理
                return CompletableFuture.completedFuture(AuthResult.CONTINUE);
            }

        } catch (Exception e) {
            // 数据库异常，返回 CONTINUE 让下一个认证器处理
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                // 不关闭连接，回收到连接池
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    @Override
    public String getName() {
        return AUTH_TYPE_MYSQL;
    }
}
