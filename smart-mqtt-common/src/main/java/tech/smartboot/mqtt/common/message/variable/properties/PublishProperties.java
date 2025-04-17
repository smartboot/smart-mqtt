/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message.variable.properties;

import java.util.List;

import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.CONTENT_TYPE_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.CORRELATION_DATA_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.MESSAGE_EXPIRY_INTERVAL_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.PAYLOAD_FORMAT_INDICATOR_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.RESPONSE_TOPIC_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.TOPIC_ALIAS_BIT;
import static tech.smartboot.mqtt.common.util.MqttPropertyConstant.USER_PROPERTY_BIT;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/5
 */
public class PublishProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = PAYLOAD_FORMAT_INDICATOR_BIT | MESSAGE_EXPIRY_INTERVAL_BIT | TOPIC_ALIAS_BIT | RESPONSE_TOPIC_BIT | CORRELATION_DATA_BIT | USER_PROPERTY_BIT | SUBSCRIPTION_IDENTIFIER_BIT | CONTENT_TYPE_BIT;

    public PublishProperties() {
        super(PROPERTIES_BITS);
    }

    public byte getPayloadFormatIndicator() {
        return properties.getPayloadFormatIndicator();
    }

    public void setPayloadFormatIndicator(byte payloadFormatIndicator) {
        properties.setPayloadFormatIndicator(payloadFormatIndicator);
    }

    public int getMessageExpiryInterval() {
        return properties.getMessageExpiryInterval();
    }

    public void setMessageExpiryInterval(int messageExpiryInterval) {
        properties.setMessageExpiryInterval(messageExpiryInterval);
    }

    public int getTopicAlias() {
        return properties.getTopicAlias();
    }

    public void setTopicAlias(int topicAlias) {
        properties.setTopicAlias(topicAlias);
    }

    public String getResponseTopic() {
        return properties.getResponseTopic();
    }

    public void setResponseTopic(String responseTopic) {
        properties.setResponseTopic(responseTopic);
    }

    public byte[] getCorrelationData() {
        return properties.getCorrelationData();
    }

    public void setCorrelationData(byte[] correlationData) {
        properties.setCorrelationData(correlationData);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }

    public int getSubscriptionIdentifier() {
        return properties.getSubscriptionIdentifier();
    }

    public void setSubscriptionIdentifier(int subscriptionIdentifier) {
        properties.setSubscriptionIdentifier(subscriptionIdentifier);
    }

    public String getContentType() {
        return properties.getContentType();
    }

    public void setContentType(String contentType) {
        properties.setContentType(contentType);
    }

}

