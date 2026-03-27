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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * MySQL 认证器
 * 从 MySQL 数据库查询用户凭证进行认证，适用于传统企业应用和已有数据库系统
 * <p>
 * 配置选项：
 * - url: JDBC 连接 URL（必填，如 jdbc:mysql://localhost:3306/mqtt）
 * - username: 数据库用户名（必填）
 * - password: 数据库密码（必填）
 * <p>
 * 其他参数使用默认值：
 * - tableName: mqtt_users
 * - usernameColumn: username
 * - passwordColumn: password
 * - maxConnections: 5
 * - minIdle: 2
 * - connectionTimeout: 3000ms
 * <p>
 * 数据表结构示例：
 * CREATE TABLE mqtt_users (
 *   id INT PRIMARY KEY AUTO_INCREMENT,
 *   username VARCHAR(64) NOT NULL UNIQUE,
 *   password VARCHAR(256) NOT NULL,
 *   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class MysqlAuthenticator extends AbstractAuthenticator {
    private static final Logger logger = Logger.getLogger(MysqlAuthenticator.class.getName());

    // HikariCP 数据源
    private HikariDataSource dataSource;
    private volatile boolean initialized = false;
    private final MysqlConfig config;

    // 默认配置常量
    private static final String DEFAULT_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_TABLE_NAME = "mqtt_users";
    private static final String DEFAULT_USERNAME_COLUMN = "username";
    private static final String DEFAULT_PASSWORD_COLUMN = "password";
    private static final int DEFAULT_MAX_CONNECTIONS = 5;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 3000;
    private static final long DEFAULT_IDLE_TIMEOUT = 600000;
    private static final long DEFAULT_VALIDATION_TIMEOUT = 1000;

    public MysqlAuthenticator(MysqlConfig mysql) {
        this.config = mysql;
    }

    @Override
    public void initialize() {
        // 验证配置参数
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            throw new IllegalArgumentException("MySQL URL is required");
        }
        if (config.getUsername() == null || config.getUsername().isEmpty()) {
            throw new IllegalArgumentException("MySQL username is required");
        }
        if (config.getPassword() == null || config.getPassword().isEmpty()) {
            throw new IllegalArgumentException("MySQL password is required");
        }
        
        // 初始化并配置 HikariCP 连接池
        initHikariDataSource();
        
        initialized = true;
    }

    /**
     * 初始化 HikariCP 数据源
     */
    private void initHikariDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 基本配置
        config.setJdbcUrl(this.config.getUrl());
        config.setUsername(this.config.getUsername());
        config.setPassword(this.config.getPassword());
        
        // 驱动配置
        config.setDriverClassName(DEFAULT_DRIVER_CLASS);
        
        // 连接池配置
        config.setMaximumPoolSize(DEFAULT_MAX_CONNECTIONS);
        config.setMinimumIdle(DEFAULT_MIN_IDLE);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT);
        
        // 连接测试查询
        config.setConnectionTestQuery("SELECT 1");
        
        // 添加 MySQL 特定优化配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // 设置连接池名称
        config.setPoolName("MysqlAuth-Pool");
        
        // 启用 JMX 监控（可选）
        config.setRegisterMbeans(true);
        
        // 创建数据源
        dataSource = new HikariDataSource(config);
        
        logger.info("HikariCP connection pool initialized with max " + DEFAULT_MAX_CONNECTIONS + 
                   " connections, min idle " + DEFAULT_MIN_IDLE);
    }

    @Override
    public void destroy() {
        initialized = false;
        
        // 关闭 HikariCP 数据源
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool destroyed");
        }
    }

    @Override
    public CompletableFuture<AuthResult> authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] passwordBytes = getPassword(message);

        if (username == null || username.isEmpty() || passwordBytes == null || passwordBytes.length == 0) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        if (!initialized) {
            logger.log(Level.WARNING, "MySQL authenticator not initialized yet");
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        // 使用 try-with-resources 自动管理连接
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(buildSQL())) {
            
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String expectedPassword = rs.getString(1);
                    
                    // 使用父类的统一密码验证逻辑
                    return doAuthenticate(session, message, expectedPassword);
                } else {
                    // 用户不存在，让下一个认证器处理
                    return CompletableFuture.completedFuture(AuthResult.CONTINUE);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database authentication failed", e);
            // 数据库异常，返回 CONTINUE 让下一个认证器处理
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }
    }

    /**
     * 构建 SQL 查询语句
     */
    private String buildSQL() {
        return "SELECT " + quoteIdentifier(DEFAULT_PASSWORD_COLUMN) + " FROM " + 
               quoteIdentifier(DEFAULT_TABLE_NAME) +
               " WHERE " + quoteIdentifier(DEFAULT_USERNAME_COLUMN) + " = ?";
    }

    /**
     * 引用标识符（防止 SQL 注入）
     */
    private String quoteIdentifier(String identifier) {
        // MySQL 使用反引号引用标识符
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String getName() {
        return AUTH_TYPE_MYSQL;
    }
}
