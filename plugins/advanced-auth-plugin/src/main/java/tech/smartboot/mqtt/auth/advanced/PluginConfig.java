/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置类
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class PluginConfig {

    /**
     * 认证器出错时是否停止（默认true）
     */
    private boolean stopOnError = true;

    /**
     * 是否启用匿名访问（默认false）
     */
    private boolean allowAnonymous = false;

    /**
     * Redis 认证器配置
     */
    private RedisConfig redis;

    /**
     * MySQL 认证器配置
     */
    private MysqlConfig mysql;

    /**
     * HTTP 认证器配置
     */
    private HttpConfig http;

    /**
     * 认证链顺序（按此顺序执行认证器，默认按 redis -> mysql -> http）
     */
    private List<String> chain;

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public MysqlConfig getMysql() {
        return mysql;
    }

    public void setMysql(MysqlConfig mysql) {
        this.mysql = mysql;
    }

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    /**
     * 获取认证链顺序
     * 如果未配置，则默认返回 redis -> mysql -> html
     */
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    /**
     * 认证器基础配置
     */
    public static abstract class AuthenticatorConfig {
        /**
         * 密码编码方式：plain, sha256, base64
         */
        private String passwordEncoder = "plain";

        public String getPasswordEncoder() {
            return passwordEncoder;
        }

        public void setPasswordEncoder(String passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }
    }

    /**
     * Redis 认证器配置
     */
    public static class RedisConfig extends AuthenticatorConfig {
        /**
         * Redis 主机地址
         */
        private String address = "redis://localhost:6379";


        /**
         * Redis 密码
         */
        private String password;

        /**
         * Redis 数据库索引
         */
        private int database = 0;

        /**
         * Key 前缀
         */
        private String keyPrefix = "mqtt:auth:";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 2000;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
    }

    /**
     * MySQL 认证器配置
     */
    public static class MysqlConfig extends AuthenticatorConfig {
        /**
         * 数据库连接 URL
         */
        private String url;

        /**
         * 数据库用户名
         */
        private String username;

        /**
         * 数据库密码
         */
        private String password;

        /**
         * JDBC 驱动类名
         */
        private String driverClass = "com.mysql.cj.jdbc.Driver";

        /**
         * 表名
         */
        private String tableName = "mqtt_users";

        /**
         * 用户名列
         */
        private String usernameColumn = "username";

        /**
         * 密码列
         */
        private String passwordColumn = "password";

        /**
         * WHERE 子句
         */
        private String whereClause;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 3000;

        /**
         * 最大连接数
         */
        private int maxConnections = 5;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getUsernameColumn() {
            return usernameColumn;
        }

        public void setUsernameColumn(String usernameColumn) {
            this.usernameColumn = usernameColumn;
        }

        public String getPasswordColumn() {
            return passwordColumn;
        }

        public void setPasswordColumn(String passwordColumn) {
            this.passwordColumn = passwordColumn;
        }

        public String getWhereClause() {
            return whereClause;
        }

        public void setWhereClause(String whereClause) {
            this.whereClause = whereClause;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }

    /**
     * HTTP 认证器配置
     */
    public static class HttpConfig extends AuthenticatorConfig {
        /**
         * 认证接口 URL
         */
        private String url;

        /**
         * HTTP 方法：GET, POST
         */
        private String method = "POST";

        /**
         * Content-Type
         */
        private String contentType = "application/json";

        /**
         * 请求超时时间（毫秒）
         */
        private int timeout = 5000;

        /**
         * 用户名字段名
         */
        private String usernameField = "username";

        /**
         * 密码字段名
         */
        private String passwordField = "password";

        /**
         * 成功响应码
         */
        private int successCode = 200;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getUsernameField() {
            return usernameField;
        }

        public void setUsernameField(String usernameField) {
            this.usernameField = usernameField;
        }

        public String getPasswordField() {
            return passwordField;
        }

        public void setPasswordField(String passwordField) {
            this.passwordField = passwordField;
        }

        public int getSuccessCode() {
            return successCode;
        }

        public void setSuccessCode(int successCode) {
            this.successCode = successCode;
        }
    }
}
