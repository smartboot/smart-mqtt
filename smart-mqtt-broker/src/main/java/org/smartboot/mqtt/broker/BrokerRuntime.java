/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

/**
 * MQTT 运行时信息
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public final class BrokerRuntime {
    /**
     * 节点名称
     */
    private String name;
    /**
     * 进程ID
     */
    private String pid;
    /**
     * Broker IP地址
     */
    private String ipAddress;
    /**
     * 启动时间
     */
    private long startTime;

    BrokerRuntime() {
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
