/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.mqtt.plugin.openapi.enums.BrokerNodeTypeEnum;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/11/30
 */
class Config {


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


    public String getClusterEndpoint() {
        return clusterEndpoint;
    }

    public void setClusterEndpoint(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
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
}
