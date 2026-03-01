/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.bench;

/**
 * 发布压测配置
 *
 * 公共参数（host, port, topicCount, qos, payloadSize）已提取到 PluginConfig
 *
 * @author 三刀
 * @version v1.5.1
 */
public class PublishConfig {

    /**
     * 发布者数量
     */
    private int connections = 1000;

    /**
     * 每次发布的消息数量
     */
    private int publishCount = 1;

    /**
     * 发布间隔（毫秒）
     */
    private int period = 1;

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public int getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(int publishCount) {
        this.publishCount = publishCount;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}