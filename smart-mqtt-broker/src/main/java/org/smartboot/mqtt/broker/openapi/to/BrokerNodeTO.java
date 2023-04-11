/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.openapi.to;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class BrokerNodeTO {
    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点状态
     *
     * @see org.smartboot.mqtt.broker.openapi.enums.BrokerStatueEnum
     */
    private String status;

    /**
     * 运行时长
     */
    private String runtime;

    /**
     * broker版本
     */
    private String version;
    /**
     * Broker IP地址
     */
    private String ipAddress;

    /**
     * Broker端口号
     */
    private int port;

    /**
     * 服务进程
     */
    private String pid;

    /**
     * 内存使用率
     */
    private int memory;

    /**
     * CPU使用率
     */
    private int cpu;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
