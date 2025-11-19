/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.client.processor;

import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {

    @Override
    public void process(MqttClient session, MqttPublishMessage mqttPublishMessage) {
        MqttQoS mqttQoS = mqttPublishMessage.getFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                session.accepted(mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(session, mqttPublishMessage);
                break;
            default:
                throw new IllegalStateException("unsupported qos level: " + mqttQoS);
        }

    }


    private void processQos1(MqttClient mqttClient, MqttPublishMessage mqttPublishMessage) {
        mqttClient.accepted(mqttPublishMessage);
        //todo
        ReasonProperties properties = null;
        if (mqttClient.getMqttVersion() == MqttVersion.MQTT_5) {
            properties = new ReasonProperties();
        }
        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(mqttPublishMessage.getVariableHeader().getPacketId(), properties);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(variableHeader);
        mqttClient.write(pubAckMessage, false);
    }

    private void processQos2(MqttClient session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getVariableHeader().getPacketId();
        //todo
        ReasonProperties properties = null;
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            properties = new ReasonProperties();
        }
        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(messageId, properties);

        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(variableHeader);
        session.write(pubRecMessage, mqttPublishMessage);
    }

}
