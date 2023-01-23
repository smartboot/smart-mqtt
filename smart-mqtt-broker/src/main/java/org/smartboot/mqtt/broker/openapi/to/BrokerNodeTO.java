package org.smartboot.mqtt.broker.openapi.to;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class BrokerNodeTO {
    /**
     * 节点名称
     */
    private String node;

    /**
     * 节点状态
     */
    private int status;

    /**
     * 运行时长
     */
    private String runtime;

    /**
     * broker版本
     */
    private String version;

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

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
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
}
