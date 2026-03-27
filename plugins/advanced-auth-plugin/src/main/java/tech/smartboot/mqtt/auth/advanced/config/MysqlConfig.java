package tech.smartboot.mqtt.auth.advanced.config;

/**
 * MySQL 认证器配置
 */
public class MysqlConfig extends AuthenticatorConfig {
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
