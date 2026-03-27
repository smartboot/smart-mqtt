package tech.smartboot.mqtt.auth.advanced.config;

/**
 * 认证器基础配置
 */
public abstract class AuthenticatorConfig {
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