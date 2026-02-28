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
 * 订阅压测配置
 *
 * @author 三刀
 * @version v1.5.1
 */
public class SubscribeConfig {

    /**
     * MQTT服务器主机
     */
    private String host = "127.0.0.1";

    /**
     * MQTT服务器端口
     */
    private int port = 1883;

    /**
     * 连接数
     */
    private int connections = 1000;

    /**
     * 主题数量
     */
    private int topicCount = 128;

    /**
     * QoS等级: 0-AtMostOnce, 1-AtLeastOnce, 2-ExactlyOnce
     */
    private int qos = 0;

    /**
     * 发布者数量
     */
    private int publisherCount = 1;

    /**
     * 每次发布的消息数量
     */
    private int publishCount = 1;

    /**
     * 发布间隔（毫秒）
     */
    private int publishPeriod = 1;

    /**
     * 消息 payload 大小（字节）
     */
    private int payloadSize = 128;

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

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getPublisherCount() {
        return publisherCount;
    }

    public void setPublisherCount(int publisherCount) {
        this.publisherCount = publisherCount;
    }

    public int getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(int publishCount) {
        this.publishCount = publishCount;
    }

    public int getPublishPeriod() {
        return publishPeriod;
    }

    public void setPublishPeriod(int publishPeriod) {
        this.publishPeriod = publishPeriod;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }
}