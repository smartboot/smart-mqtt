package tech.smartboot.mqtt.auth.advanced.config;

/**
 * HTTP 认证器配置
 */
public class HttpConfig extends AuthenticatorConfig {
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