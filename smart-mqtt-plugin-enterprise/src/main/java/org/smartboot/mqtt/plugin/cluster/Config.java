package org.smartboot.mqtt.plugin.cluster;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/11/30
 */
class Config {


    /**
     * @see org.smartboot.mqtt.plugin.openapi.enums.BrokerNodeTypeEnum
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
