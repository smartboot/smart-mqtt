package org.smartboot.mqtt.broker.config;


public class PluginConfig {
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
