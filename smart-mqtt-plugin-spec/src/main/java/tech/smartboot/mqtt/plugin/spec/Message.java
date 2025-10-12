/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec;

import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public final class Message {
    /**
     * 负载数据
     */
    private final byte[] payload;

    /**
     * 主题
     */
    private final BrokerTopic topic;

    private final boolean retained;
    /**
     * 存储的消息偏移量
     */
    private long offset;

    private final MqttQoS qos;
    /**
     * 本条消息可推送的次数
     */
    private AtomicInteger pushSemaphore;

    public Message(MqttPublishMessage message, BrokerTopic topic) {
        this.payload = message.getPayload().getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = topic;
        this.qos = message.getFixedHeader().getQosLevel();
    }

    public Message(BrokerTopic topic, MqttQoS qos, byte[] message, boolean retained) {
        this.payload = message;
        this.retained = retained;
        this.topic = topic;
        this.qos = qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public BrokerTopic getTopic() {
        return topic;
    }

    public boolean isRetained() {
        return retained;
    }

    public long getOffset() {
        return offset;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int decrementAndGet() {
        return pushSemaphore.decrementAndGet();
    }

    public void setPushSemaphore(int pushSemaphore) {
        this.pushSemaphore = new AtomicInteger(pushSemaphore);
    }
}
