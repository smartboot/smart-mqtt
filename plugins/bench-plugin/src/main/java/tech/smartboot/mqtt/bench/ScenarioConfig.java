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
 * 压测场景配置
 *
 * 场景特有配置，公共参数（host, port, topicCount, payloadSize）在 PluginConfig 中统一配置
 *
 * @author 三刀
 * @version v1.5.2
 */
public class ScenarioConfig {

    /**
     * 场景名称
     */
    private String name;

    /**
     * 订阅者数量
     * 设置为0则不启动订阅者
     */
    private int subscribers = 1000;

    /**
     * 发布者数量
     * 设置为0则不启动发布者
     */
    private int publishers = 1;

    /**
     * 每个连接每秒推送的消息数
     */
    private int rate = 1000;

    /**
     * 发布QoS等级: 0-AtMostOnce, 1-AtLeastOnce, 2-ExactlyOnce
     */
    private int publishQos = 0;

    /**
     * 订阅QoS等级: 0-AtMostOnce, 1-AtLeastOnce, 2-ExactlyOnce
     */
    private int subscribeQos = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(int subscribers) {
        this.subscribers = subscribers;
    }

    public int getPublishers() {
        return publishers;
    }

    public void setPublishers(int publishers) {
        this.publishers = publishers;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getPublishQos() {
        return publishQos;
    }

    public void setPublishQos(int publishQos) {
        this.publishQos = publishQos;
    }

    public int getSubscribeQos() {
        return subscribeQos;
    }

    public void setSubscribeQos(int subscribeQos) {
        this.subscribeQos = subscribeQos;
    }
}