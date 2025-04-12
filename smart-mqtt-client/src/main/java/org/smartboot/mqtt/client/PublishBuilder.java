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

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

public final class PublishBuilder implements MqttMessageBuilders.MessageBuilder<MqttPublishMessage> {
    public static PublishBuilder builder() {
        return new PublishBuilder();
    }

    private byte[] topic;
    private boolean retained;
    private MqttQoS qos;
    private byte[] payload;
    private int packetId = -1;
    private PublishProperties publishProperties;

    PublishBuilder() {
    }

    public PublishBuilder topicName(String topic) {
        this.topic = MqttCodecUtil.encodeUTF8(topic);
        return this;
    }

    public PublishBuilder retained(boolean retained) {
        this.retained = retained;
        return this;
    }

    public PublishBuilder qos(MqttQoS qos) {
        this.qos = qos;
        return this;
    }

    public PublishBuilder payload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    public PublishBuilder packetId(int packetId) {
        this.packetId = packetId;
        return this;
    }

    @Override
    public MqttQoS qos() {
        return qos;
    }

    public PublishBuilder publishProperties(PublishProperties publishProperties) {
        this.publishProperties = publishProperties;
        return this;
    }

    public int getPacketId() {
        return packetId;
    }

    public MqttPublishMessage build() {
        MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(packetId, topic, publishProperties);
        return new MqttPublishMessage(MqttFixedHeader.getInstance(MqttMessageType.PUBLISH, false, qos.value(), retained), mqttVariableHeader, payload);
    }
}