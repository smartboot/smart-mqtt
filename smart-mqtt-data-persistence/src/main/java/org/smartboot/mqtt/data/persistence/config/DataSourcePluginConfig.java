package org.smartboot.mqtt.data.persistence.config;


import org.smartboot.mqtt.broker.config.PluginConfig;

public class DataSourcePluginConfig extends PluginConfig {
    private String password;
    
    private int timeout = 1000;
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
