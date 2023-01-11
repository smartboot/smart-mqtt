package org.smartboot.mqtt.common.message.variable.properties;

import org.smartboot.mqtt.common.message.MqttCodecUtil;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/5
 */
public class PublishProperties {
    private final MqttProperties properties;

    public PublishProperties(MqttProperties properties) {
        this.properties = properties;
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

    public byte[] getResponseTopicBytes() {
        return responseTopicBytes;
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

    public byte[] getContentTypeBytes() {
        return contentTypeBytes;
    }

    public void setContentType(String contentType) {
        properties.setContentType(contentType);
    }

    private byte[] responseTopicBytes;
    private byte[] contentTypeBytes;

    public void decode() {
        if (getResponseTopic() != null) {
            responseTopicBytes = MqttCodecUtil.encodeUTF8(getResponseTopic());
        }
        if (getContentType() != null) {
            contentTypeBytes = MqttCodecUtil.encodeUTF8(getContentType());
        }
    }
}

