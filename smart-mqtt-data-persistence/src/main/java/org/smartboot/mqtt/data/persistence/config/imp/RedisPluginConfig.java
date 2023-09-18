package org.smartboot.mqtt.data.persistence.config.imp;


import org.smartboot.mqtt.data.persistence.config.PluginConfig;

public class RedisPluginConfig extends PluginConfig {
    private String password;
    private int timeout = 1000;
    private boolean simple = true;
    
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
    
    public boolean isSimple() {
        return simple;
    }
    
    public void setSimple(boolean simple) {
        this.simple = simple;
    }
}
