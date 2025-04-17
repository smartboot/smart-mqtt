/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.to;


import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Date;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
public class ConnectionTO {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 状态
     */
    private String status;

    /**
     * IP地址
     */
    @JSONField(name = "ip_address")
    private String ipAddress;

    /**
     * Broker IP地址
     */
    @JSONField(name = "nodeId")
    private String nodeId;
    /**
     * 心跳
     */
    private int keepalive;


    @JSONField(name = "clean_start")
    private boolean cleanStart;

    /**
     * 会话过期时间
     */
    @JSONField(name = "expiry_interval")
    private int expiryInterval;

    /**
     * 连接时间
     */
    @JSONField(name = "connect_time", format = "yyyy-MM-dd HH:mm:ss")
    private Date connectTime;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public int getKeepalive() {
        return keepalive;
    }

    public void setKeepalive(int keepalive) {
        this.keepalive = keepalive;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public void setCleanStart(boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public int getExpiryInterval() {
        return expiryInterval;
    }

    public void setExpiryInterval(int expiryInterval) {
        this.expiryInterval = expiryInterval;
    }

    public Date getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(Date connectTime) {
        this.connectTime = connectTime;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
