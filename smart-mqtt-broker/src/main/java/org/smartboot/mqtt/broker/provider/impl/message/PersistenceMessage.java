/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider.impl.message;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public class PersistenceMessage extends ToString {
    /**
     * 负载数据
     */
    private final byte[] payload;
    /**
     * 主题
     */
    private final String topic;

    private final boolean retained;
    /**
     * 存储的消息偏移量
     */
    private long offset;

    private final MqttQoS qos;
    /**
     * 消息存储时间
     */
    private final long createTime = System.currentTimeMillis();

    private final String clientId;

    public PersistenceMessage(MqttSession session, MqttPublishMessage message) {
        this.clientId = session.getClientId();
        this.payload = message.getPayload().getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = message.getVariableHeader().getTopicName();
        this.qos = message.getFixedHeader().getQosLevel();
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public boolean isRetained() {
        return retained;
    }

    public long getCreateTime() {
        return createTime;
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

    public String getClientId() {
        return clientId;
    }
}
