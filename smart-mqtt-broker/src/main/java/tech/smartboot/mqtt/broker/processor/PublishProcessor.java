/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.processor;

import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttReasonCode;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor extends AuthorizedMqttProcessor<MqttPublishMessage> {

    @Override
    public void process0(BrokerContextImpl context, MqttSessionImpl session, MqttPublishMessage mqttPublishMessage) {
//        LOGGER.info("receive publish message:{}", mqttPublishMessage);

        MqttQoS mqttQoS = mqttPublishMessage.getFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                session.accepted(mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(context, session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(context, session, mqttPublishMessage);
                break;
            default:
                throw new IllegalStateException("unsupported qos level: " + mqttQoS);
        }

    }

    private void processQos1(BrokerContext context, MqttSessionImpl session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getVariableHeader().getPacketId();
        //给 publisher 回响应
        MqttPubQosVariableHeader variableHeader;
        //todo
        byte reasonCode = 0;
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            //消息被接收，但没有订阅者。只有服务端会发送此原因码。如果服务端得知没有匹配的订阅者，服务端可以使用此原因码代替0x00（成功）。
            if (context.getOrCreateTopic(mqttPublishMessage.getVariableHeader().getTopicName()).subscribeCount() == 0) {
                reasonCode = MqttReasonCode.NO_MATCHING_SUBSCRIBERS.getCode();
            }
        }
        if (reasonCode != 0) {
            ReasonProperties properties = new ReasonProperties();
            variableHeader = new MqttPubQosVariableHeader(messageId, properties);
            variableHeader.setReasonCode(reasonCode);
        } else {
            variableHeader = new MqttPubQosVariableHeader(messageId, null);
        }
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(variableHeader);
        session.write(pubAckMessage, false);
        // 消息投递至消息总线
        session.accepted(mqttPublishMessage);
    }

    private void processQos2(BrokerContext context, MqttSessionImpl session, MqttPublishMessage mqttPublishMessage) {
        MqttPubQosVariableHeader variableHeader;
        //todo
        byte reasonCode = 0;
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            //消息被接收，但没有订阅者。只有服务端会发送此原因码。如果服务端得知没有匹配的订阅者，服务端可以使用此原因码代替0x00（成功）。
            if (context.getOrCreateTopic(mqttPublishMessage.getVariableHeader().getTopicName()).subscribeCount() == 0) {
                reasonCode = MqttReasonCode.NO_MATCHING_SUBSCRIBERS.getCode();
            }
        }
        if (reasonCode != 0) {
            ReasonProperties properties = new ReasonProperties();
            variableHeader = new MqttPubQosVariableHeader(mqttPublishMessage.getVariableHeader().getPacketId(), properties);
            variableHeader.setReasonCode(reasonCode);
        } else {
            variableHeader = new MqttPubQosVariableHeader(mqttPublishMessage.getVariableHeader().getPacketId(), null);
        }
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(variableHeader);

        //响应监听
        session.write(pubRecMessage, mqttPublishMessage);
    }
}
