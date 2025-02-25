
package org.smartboot.mqtt.plugin.dao.query;

import java.util.Date;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/24
 */
public class ConnectionQuery extends Query {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 状态：已连接、认证失败、已离线
     */
    private String status;
    /**
     * Broker IP地址
     */
    private List<String> brokers;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 连接时间
     */
    private Date connectStartTime;

    /**
     * 连接时间
     */
    private Date connectEndTime;

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

    public List<String> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<String> brokers) {
        this.brokers = brokers;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getConnectStartTime() {
        return connectStartTime;
    }

    public void setConnectStartTime(Date connectStartTime) {
        this.connectStartTime = connectStartTime;
    }

    public Date getConnectEndTime() {
        return connectEndTime;
    }

    public void setConnectEndTime(Date connectEndTime) {
        this.connectEndTime = connectEndTime;
    }
}
