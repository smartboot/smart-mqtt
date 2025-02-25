package org.smartboot.mqtt.plugin.dao.model;

public class PluginConfigDO {
    private int id;

    /**
     * 插件类型
     */
    private String pluginType;

    /**
     * 插件状态
     */
    private int status;
    /**
     * 配置内容
     */
    private String config;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPluginType() {
        return pluginType;
    }

    public void setPluginType(String pluginType) {
        this.pluginType = pluginType;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
