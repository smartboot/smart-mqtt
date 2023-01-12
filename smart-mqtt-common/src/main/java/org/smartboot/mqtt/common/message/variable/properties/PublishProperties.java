package org.smartboot.mqtt.common.message.variable.properties;

import java.util.List;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.*;

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

