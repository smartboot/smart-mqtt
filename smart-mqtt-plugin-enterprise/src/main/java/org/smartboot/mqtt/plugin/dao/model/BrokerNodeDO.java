package org.smartboot.mqtt.plugin.dao.model;

import java.util.Date;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/4
 */
public class BrokerNodeDO {
    /**
     * 节点名称，唯一性
     */
    private String nodeId;

    /**
     * 进程信息
     */
    private String process;
    /**
     * smart-mqtt.yaml配置
     */
    private String config;

    /**
     * 状态
     */
    private String status;

    /**
     * Broker IP地址
     */
    private String ipAddress;
    /**
     * 端口号
     */
    private int port;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 核心节点ID
     */
    private String coreNodeId;

    /**
     * 对接的集群节点
     */
    private String clusterEndpoint;

    /**
     * 服务启动时间
     */
    private Date startTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date editTime;

    private String metric;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getEditTime() {
        return editTime;
    }

    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getClusterEndpoint() {
        return clusterEndpoint;
    }

    public void setClusterEndpoint(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getCoreNodeId() {
        return coreNodeId;
    }

    public void setCoreNodeId(String coreNodeId) {
        this.coreNodeId = coreNodeId;
    }
}
