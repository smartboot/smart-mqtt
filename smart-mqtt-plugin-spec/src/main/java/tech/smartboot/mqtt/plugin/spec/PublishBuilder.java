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

import tech.smartboot.mqtt.common.enums.MqttMessageType;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.message.MessageBuilder;
import tech.smartboot.mqtt.common.message.MqttFixedHeader;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.PublishProperties;

public final class PublishBuilder implements MessageBuilder<MqttPublishMessage> {
    public static PublishBuilder builder() {
        return new PublishBuilder();
    }

    private BrokerTopic topic;
    private boolean retained;
    private MqttQoS qos;
    private byte[] payload;
    private int packetId = -1;
    private PublishProperties publishProperties;

    PublishBuilder() {
    }

    public PublishBuilder topic(BrokerTopic topic) {
        this.topic = topic;
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

    public MqttPublishMessage build() {
        MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(packetId, topic.encodedTopicBytes(), publishProperties);
        return new MqttPublishMessage(MqttFixedHeader.getInstance(MqttMessageType.PUBLISH, false, qos.value(), retained), mqttVariableHeader, payload);
    }
}