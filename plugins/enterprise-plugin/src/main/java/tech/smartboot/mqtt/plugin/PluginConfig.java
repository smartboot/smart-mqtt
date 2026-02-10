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

/**
 * @author 三刀
 * @version v1.0 4/27/25
 */
public class PluginConfig {
    private HttpConfig http;

    private DataBaseConfig database;
    private OpenAI openAI;
    private String registry;


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


    public DataBaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DataBaseConfig dataBase) {
        this.database = dataBase;
    }

    public OpenAI getOpenAI() {
        return openAI;
    }

    public void setOpenAI(OpenAI openAI) {
        this.openAI = openAI;
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
    }

    public static class OpenAI {
        private String url;
        private String apiKey;
        private String model;

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
    }

}
