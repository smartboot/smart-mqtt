/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * @author 三刀
 * @version v1.0 4/27/25
 */
public class PluginConfig {
    private HttpConfig http;

    private DataBaseConfig database;
    private OpenAI openai;

    private String registry;

    /**
     * 显示的指标项列表
     */
    private List<String> showMetrics;

    /**
     * 连接防抖配置
     */
    private FlappingConfig flapping;


    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig httpConfig) {
        this.http = httpConfig;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public List<String> getShowMetrics() {
        return showMetrics;
    }

    public void setShowMetrics(List<String> showMetrics) {
        this.showMetrics = showMetrics;
    }

    public DataBaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DataBaseConfig dataBase) {
        this.database = dataBase;
    }

    public OpenAI getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAI openai) {
        this.openai = openai;
    }

    public FlappingConfig getFlapping() {
        return flapping;
    }

    public void setFlapping(FlappingConfig flapping) {
        this.flapping = flapping;
    }

    public static class HttpConfig {
        private String host;
        private int port = 18083;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class DataBaseConfig {

        /**
         * 数据库类型：h2、mysql
         */
        private String dbType = "h2_mem";

        private String url;
        @JSONField(serialize = false)
        private String username;
        @JSONField(serialize = false)
        private String password;

        /**
         * 连接记录开关
         */
        private boolean connectRecord = false;

        /**
         * 订阅记录开关
         */
        private boolean subscribeRecord = false;

        /**
         * 指标记录开关
         */
        private boolean metricRecord = false;

        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isConnectRecord() {
            return connectRecord;
        }

        public void setConnectRecord(boolean connectRecord) {
            this.connectRecord = connectRecord;
        }

        public boolean isSubscribeRecord() {
            return subscribeRecord;
        }

        public void setSubscribeRecord(boolean subscribeRecord) {
            this.subscribeRecord = subscribeRecord;
        }

        public boolean isMetricRecord() {
            return metricRecord;
        }

        public void setMetricRecord(boolean metricRecord) {
            this.metricRecord = metricRecord;
        }
    }

    public static class OpenAI {
        private String url;
        private String apiKey;
        private String model;
        private List<Mcp> mcp;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<Mcp> getMcp() {
            return mcp;
        }

        public void setMcp(List<Mcp> mcp) {
            this.mcp = mcp;
        }
    }

    public static class Mcp {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * 连接防抖配置
     * 用于检测和限制频繁连接/断开的客户端
     */
    public static class FlappingConfig {
        /**
         * 是否启用防抖检测
         */
        private boolean enable = false;

        /**
         * 检测时间窗口（单位：秒）
         * 在此时间窗口内统计客户端的连接/断开次数
         */
        private int thresholdDuration = 60;

        /**
         * 在时间窗口内允许的最大连接次数
         * 超过此次数将被判定为抖动客户端
         */
        private int thresholdCount = 5;

        /**
         * 封禁时间（单位：秒）
         * 被判定为抖动客户端后将被封禁的时间
         */
        private int banTime = 300;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public int getThresholdDuration() {
            return thresholdDuration;
        }

        public void setThresholdDuration(int thresholdDuration) {
            this.thresholdDuration = thresholdDuration;
        }

        public int getThresholdCount() {
            return thresholdCount;
        }

        public void setThresholdCount(int thresholdCount) {
            this.thresholdCount = thresholdCount;
        }

        public int getBanTime() {
            return banTime;
        }

        public void setBanTime(int banTime) {
            this.banTime = banTime;
        }
    }
}
