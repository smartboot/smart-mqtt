package org.smartboot.mqtt.broker;

/**
 * MQTT 运行时信息
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public final class BrokerRuntime {
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
}
