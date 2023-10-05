/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.bridge.redis.nodeinfo;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.Map;

public class BrokerNodeInfo {
    /**
     * 节点名称
     */
    private String name;

    
    /**
     * broker版本
     */
    private String version;
    /**
     * Broker IP地址
     */
    private String ipAddress;
    
    
    /**
     * 服务进程
     */
    private String pid;
    
    /**
     * 内存使用率
     */
    private String memory;
    
    /**
     * CPU使用率
     */
    private String cpu;
    
    /**
     * 最近启动时间
     */
    private String recentTime;
    
    /**
     * broker创建时间
     */
    private String createTime;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getPid() {
        return pid;
    }
    
    public void setPid(String pid) {
        this.pid = pid;
    }
    
    public String getMemory() {
        return memory;
    }
    
    public void setMemory(String memory) {
        this.memory = memory;
    }
    
    public String getCpu() {
        return cpu;
    }
    
    public void setCpu(String cpu) {
        this.cpu = cpu;
    }
    
    public String getRecentTime() {
        return recentTime;
    }
    
    public void setRecentTime(String recentTime) {
        this.recentTime = recentTime;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public Map<String, String> toMap(){
        return JSON.parseObject(JSON.toJSONString(this), new TypeReference<Map<String, String>>() {});
    }
}
