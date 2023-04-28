package org.smartboot.mqtt.data.persistence.config;




public class DataSourcePluginConfig extends PluginConfig {
    private String password;
    
    private int timeout = 1000;

    private boolean base64 = false;
    
    
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
    
    public boolean isBase64() {
        return base64;
    }
    
    public void setBase64(boolean base64) {
        this.base64 = base64;
    }
}
