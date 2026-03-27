package tech.smartboot.mqtt.auth.advanced.config;

/**
 * Redis 认证器配置
 */
public class RedisConfig extends AuthenticatorConfig {
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
     * 加盐方式：用于指定盐和密码的组合方式，
     * 可选值：suffix（在密码尾部加盐）、prefix（在密码头部加盐）、disable（不启用）。
     */
    private String saltPosition;

    /**
     * 加密算法：plain、md5、sha256
     */
    private String algorithm;

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

    public String getSaltPosition() {
        return saltPosition;
    }

    public void setSaltPosition(String saltPosition) {
        this.saltPosition = saltPosition;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
