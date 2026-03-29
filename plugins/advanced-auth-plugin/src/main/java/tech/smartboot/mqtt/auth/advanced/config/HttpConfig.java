package tech.smartboot.mqtt.auth.advanced.config;

import java.util.List;

/**
 * HTTP 认证器配置
 */
public class HttpConfig {
    /**
     * 认证接口 URL
     */
    private String url;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 5000;


    private List<Header> headers;


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


    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public static class Header {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}