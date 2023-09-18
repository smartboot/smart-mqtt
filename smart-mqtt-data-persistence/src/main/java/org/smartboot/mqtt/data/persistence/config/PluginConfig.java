package org.smartboot.mqtt.data.persistence.config;


public class PluginConfig {
    private String host;
    private boolean base64 = false;
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public boolean isBase64() {
        return base64;
    }
    
    public void setBase64(boolean base64) {
        this.base64 = base64;
    }
}
