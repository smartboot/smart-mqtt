package tech.smartboot.mqtt.auth.advanced.config;

import java.util.Map;

/**
 * HTTP 认证器配置
 */
public class HttpConfig  {
    /**
     * 认证接口 URL
     */
    private String url;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 5000;


    private Map<String, String> headers;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

}