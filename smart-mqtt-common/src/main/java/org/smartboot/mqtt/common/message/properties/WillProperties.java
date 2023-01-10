package org.smartboot.mqtt.common.message.properties;

import java.util.List;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.*;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class WillProperties extends AbstractProperties {
    private static final int WILL_PROPERTIES_BITS = WILL_DELAY_INTERVAL_BIT | PAYLOAD_FORMAT_INDICATOR_BIT | MESSAGE_EXPIRY_INTERVAL_BIT | CONTENT_TYPE_BIT | RESPONSE_TOPIC_BIT | CORRELATION_DATA_BIT | USER_PROPERTY_BIT;

    public WillProperties() {
        super(WILL_PROPERTIES_BITS);
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
