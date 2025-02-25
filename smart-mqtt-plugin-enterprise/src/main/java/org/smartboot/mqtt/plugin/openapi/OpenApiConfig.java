package org.smartboot.mqtt.plugin.openapi;

import org.smartboot.mqtt.broker.plugin.Config;

public class OpenApiConfig extends Config {
    /**
     * 端口号
     */
    private int port = 18083;

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