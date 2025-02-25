package org.smartboot.mqtt.plugin.openapi.to;

import com.alibaba.fastjson2.JSONObject;

public class BridgeConfigTO {
    private int id;
    /**
     * 桥接类型
     */
    private String type;

    /**
     * 状态
     */
    private int status;

    /**
     * 配置
     */
    private JSONObject config;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(JSONObject config) {
        this.config = config;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
