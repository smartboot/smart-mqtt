package org.smartboot.mqtt.data.persistence;


import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.data.persistence.config.DataSourcePluginConfig;

public class DataPersistPlugin extends Plugin {
    private DataSourcePluginConfig config;
    
    public void setConfig(DataSourcePluginConfig config) {
        this.config = config;
    }
    
    public DataSourcePluginConfig getConfig() {
        return config;
    }
}
