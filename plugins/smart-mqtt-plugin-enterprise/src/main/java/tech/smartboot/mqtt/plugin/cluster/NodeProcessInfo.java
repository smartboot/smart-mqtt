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

public class NodeProcessInfo {
    /**
     * 进程号
     */
    private String pid;
    /**
     * CPU使用率
     */
    private int cpuUsage;
    /**
     * 内存使用率
     */
    private long memUsage;

    /**
     * 内存大小
     */
    private long memoryLimit;

    /**
     * smart-servlet版本
     */
    private String version;
    /**
     * JVM提供商
     */
    private String vmVendor;

    /**
     * JVM 版本号
     */
    private String vmVersion;

    /**
     * 操作系统
     */
    private String osName;

    /**
     * 系统架构
     */
    private String osArch;
    /**
     * 操作系统版本
     */
    private String osVersion;
    /**
     * 主机名
     */
    private String hostName;

    /**
     * 当前服务绑定IP
     */
    private String ip;

    /**
     * 服务器所在地区
     */
    private String region;


    public int getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(int cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public long getMemUsage() {
        return memUsage;
    }

    public void setMemUsage(long memUsage) {
        this.memUsage = memUsage;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVmVendor() {
        return vmVendor;
    }

    public void setVmVendor(String vmVendor) {
        this.vmVendor = vmVendor;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public void setVmVersion(String vmVersion) {
        this.vmVersion = vmVersion;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getOsArch() {
        return osArch;
    }

    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
