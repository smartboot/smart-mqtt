package org.smartboot.mqtt.data.persistence.config.imp;

import org.smartboot.mqtt.data.persistence.config.PluginConfig;

public class KafkaPluginConfig extends PluginConfig {
    private String acks = "all";
    
    private int retries = 0;
    
    private int batchSize = 16384;
    
    private int lingerMs = 1;
    
    private int buffer = 1024;
    
    public String getAcks() {
        return acks;
    }
    
    public void setAcks(String acks) {
        this.acks = acks;
    }
    
    public int getRetries() {
        return retries;
    }
    
    public void setRetries(int retries) {
        this.retries = retries;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getLingerMs() {
        return lingerMs;
    }
    
    public void setLingerMs(int lingerMs) {
        this.lingerMs = lingerMs;
    }
    
    public int getBuffer() {
        return buffer;
    }
    
    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }
    
}
