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
import tech.smartboot.mqtt.plugin.openapi.enums.BrokerNodeTypeEnum;

/**
 * @author 三刀
 * @version v1.0 4/27/25
 */
public class PluginConfig {
    private boolean enabled = true;
    @JSONField(name = "http")
    private HttpConfig httpConfig;

    @JSONField(name = "database")
    private DataBaseConfig dataBase;
    private String registry;
    /**
     * @see BrokerNodeTypeEnum
     */
    private String nodeType;

    /**
     * 连接的核心节点ID
     */
    private String coreNodeId;

    /**
     * 作为协调节点开放的集群访问地址
     */
    private String clusterEndpoint;

    public HttpConfig getHttpConfig() {
        return httpConfig;
    }

    public void setHttpConfig(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getCoreNodeId() {
        return coreNodeId;
    }

    public void setCoreNodeId(String coreNodeId) {
        this.coreNodeId = coreNodeId;
    }

    public String getClusterEndpoint() {
        return clusterEndpoint;
    }

    public void setClusterEndpoint(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
    }

    public DataBaseConfig getDataBase() {
        return dataBase;
    }

    public void setDataBase(DataBaseConfig dataBase) {
        this.dataBase = dataBase;
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

}
