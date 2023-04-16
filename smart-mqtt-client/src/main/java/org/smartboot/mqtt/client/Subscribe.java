/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.client;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.BiConsumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/7
 */
public class Subscribe {
    private final String topicFilter;
    private final MqttQoS qoS;
    private final BiConsumer<MqttClient, MqttPublishMessage> consumer;

    /**
     * This topic has unsubscribed.
     */
    private volatile boolean unsubscribed;
    private volatile long unsubscribedTime = -1L;

    public Subscribe(String topicFilter, MqttQoS qoS, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        this.topicFilter = topicFilter;
        this.qoS = qoS;
        this.consumer = consumer;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public MqttQoS getQoS() {
        return qoS;
    }

    public BiConsumer<MqttClient, MqttPublishMessage> getConsumer() {
        return consumer;
    }

    public boolean getUnsubscribed() {
        return unsubscribed;
    }

    public void setUnsubscribed(boolean unsubscribed) {
        this.unsubscribed = unsubscribed;
        this.unsubscribedTime = System.currentTimeMillis();
    }

    public long getUnsubscribedTime() {
        return unsubscribedTime;
    }
}
