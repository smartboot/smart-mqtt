/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.client.Subscribe;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.MqttUtil;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);

    @Override
    public void process(MqttClient session, MqttPublishMessage mqttPublishMessage) {
        MqttQoS mqttQoS = mqttPublishMessage.getFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(session, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(session, mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unSupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void processQos0(MqttClient mqttClient, MqttPublishMessage mqttPublishMessage) {
//        LOGGER.info("receive publish:{}", mqttPublishMessage);
        processPublishMessage(mqttPublishMessage, mqttClient);
    }

    private void processPublishMessage(MqttPublishMessage mqttPublishMessage, MqttClient mqttClient) {
        MqttPublishVariableHeader header = mqttPublishMessage.getVariableHeader();
        Subscribe subscribe = mqttClient.getMapping().get(header.getTopicName());
        if (subscribe == null) {
            subscribe = mqttClient.getSubscribes().get(header.getTopicName());
            //尝试通配符匹配
            if (subscribe == null) {
                subscribe = matchWildcardsSubscribe(mqttClient, header.getTopicName());
            }
            if (subscribe != null) {
                mqttClient.getMapping().put(header.getTopicName(), subscribe);
            }
        }

        // If unsubscribed, maybe null.
        if (subscribe != null && !subscribe.getUnsubscribed()) {
            subscribe.getConsumer().accept(mqttClient, mqttPublishMessage);
        }
    }

    private static Subscribe matchWildcardsSubscribe(MqttClient mqttClient, String topicName) {
        TopicToken publicTopicToken = new TopicToken(topicName);
        TopicToken matchToken = mqttClient.getWildcardsToken().stream().filter(topicToken -> MqttUtil.match(publicTopicToken, topicToken)).findFirst().orElse(null);
        return matchToken != null ? mqttClient.getSubscribes().get(matchToken.getTopicFilter()) : null;
    }

    private void processQos1(MqttClient mqttClient, MqttPublishMessage mqttPublishMessage) {
        processPublishMessage(mqttPublishMessage, mqttClient);
        //todo
        ReasonProperties properties = null;
        if (mqttPublishMessage.getVersion() == MqttVersion.MQTT_5) {
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
        if (mqttPublishMessage.getVersion() == MqttVersion.MQTT_5) {
            properties = new ReasonProperties();
        }
        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(messageId, properties);

        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(variableHeader);
        session.write(pubRecMessage, () -> processPublishMessage(mqttPublishMessage, session));
    }

}
