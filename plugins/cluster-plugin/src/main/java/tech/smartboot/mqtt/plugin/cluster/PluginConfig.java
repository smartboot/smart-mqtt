package tech.smartboot.mqtt.plugin.cluster;

import java.util.List;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public class PluginConfig {

    private boolean core;

    private String host;
    private int port;
    private int queueLength = 1024;
    private int queueDiscardPolicy;

    /**
     * 批量分发配置
     */
    private int batchSize = 50;           // 每批最多消息数

    private List<String> clusters;

    public boolean isCore() {
        return core;
    }

    public void setCore(boolean core) {
        this.core = core;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getClusters() {
        return clusters;
    }

    public void setClusters(List<String> clusters) {
        this.clusters = clusters;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    public int getQueueDiscardPolicy() {
        return queueDiscardPolicy;
    }

    public void setQueueDiscardPolicy(int queueDiscardPolicy) {
        this.queueDiscardPolicy = queueDiscardPolicy;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

}
