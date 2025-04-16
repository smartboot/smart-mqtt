package org.smartboot.mqtt.plugin.openapi;


import org.smartboot.mqtt.plugin.spec.PluginConfig;

public class OpenApiConfig extends PluginConfig {
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