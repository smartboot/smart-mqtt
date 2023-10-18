package org.smartboot.mqtt.data.persistence.config;

/**
* @Description: 通用持久化插件配置信息
 * @Author: learnhope
 * @Date: 2023/9/18
 */
public class PluginConfig {
    // host地址
    private String host;
    // base64编码
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
