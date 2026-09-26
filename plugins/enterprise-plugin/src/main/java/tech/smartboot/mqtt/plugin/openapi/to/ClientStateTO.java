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

/**
 * 客户端状态快照。用于集群部署时数据面节点向控制面定期上报本节点持有的客户端信息，
 * 控制面依据上报快照维护连接状态，长期未收到更新的客户端视为离线。
 * <p>
 * 语义约定：
 * <ul>
 *     <li>online=true 表示客户端当前在线；online=false 表示连接已断开但会话仍在保留期内；</li>
 *     <li>未出现在本次快照中的客户端，由控制面按超时策略判定离线；</li>
 *     <li>时间字段统一使用毫秒级时间戳，避免序列化时区问题。</li>
 * </ul>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2026/9/26
 */
public class ClientStateTO {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 客户端所连接的Broker本机IP
     */
    @JSONField(name = "broker_ip")
    private String brokerIp;

    /**
     * 客户端IP地址
     */
    @JSONField(name = "ip_address")
    private String ipAddress;

    /**
     * 心跳间隔(秒)
     */
    private int keepalive;

    /**
     * MQTT协议版本
     */
    private String mqttVersion;

    /**
     * 是否在线
     */
    private boolean online;

    @JSONField(name = "clean_start")
    private boolean cleanStart;

    /**
     * 会话过期时间(秒)
     */
    @JSONField(name = "expiry_interval")
    private int expiryInterval;

    /**
     * 建立连接时间(毫秒时间戳)
     */
    @JSONField(name = "connect_time")
    private long connectTime;

    /**
     * 最近一次活跃时间(毫秒时间戳),取最近收到的报文时间
     */
    @JSONField(name = "active_time")
    private long activeTime;

    /**
     * 本次上报时间(毫秒时间戳),由数据面生成
     */
    @JSONField(name = "report_time")
    private long reportTime;

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

    public String getBrokerIp() {
        return brokerIp;
    }

    public void setBrokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
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

    public String getMqttVersion() {
        return mqttVersion;
    }

    public void setMqttVersion(String mqttVersion) {
        this.mqttVersion = mqttVersion;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
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

    public long getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public long getReportTime() {
        return reportTime;
    }

    public void setReportTime(long reportTime) {
        this.reportTime = reportTime;
    }
}
