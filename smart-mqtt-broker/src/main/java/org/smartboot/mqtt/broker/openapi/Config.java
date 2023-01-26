package org.smartboot.mqtt.broker.openapi;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
class Config {
    /**
     * 端口号
     */
    private int port;

    /**
     * 主机地址
     */
    private String host;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
