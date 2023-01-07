package org.smartboot.mqtt.common.message.properties;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class WillProperties {
    private final MqttProperties properties;

    public WillProperties(MqttProperties properties) {
        this.properties = properties;
    }

    public int getWillDelayInterval() {
        return properties.getWillDelayInterval();
    }

    public void setWillDelayInterval(int willDelayInterval) {
        properties.setWillDelayInterval(willDelayInterval);
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

    public String getContentType() {
        return properties.getContentType();
    }

    public void setContentType(String contentType) {
        properties.setContentType(contentType);
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
}
