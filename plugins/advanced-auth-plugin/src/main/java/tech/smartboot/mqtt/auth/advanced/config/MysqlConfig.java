package tech.smartboot.mqtt.auth.advanced.config;

/**
 * MySQL 认证器配置
 * 仅支持基础配置项，其他参数使用默认值
 */
public class MysqlConfig extends AuthenticatorConfig {
    /**
     * 数据库连接 URL（必填）
     * 示例：jdbc:mysql://localhost:3306/mqtt
     */
    private String url;

    /**
     * 数据库用户名（必填）
     */
    private String username;

    /**
     * 数据库密码（必填）
     */
    private String password;

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
}
