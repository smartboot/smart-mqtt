package org.smartboot.mqtt.common.message.properties;

import org.smartboot.mqtt.common.util.MqttPropertyConstant;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/10
 */
public class PubAckProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = MqttPropertyConstant.REASON_STRING_BIT | MqttPropertyConstant.USER_PROPERTY_BIT;

    public PubAckProperties() {
        super(PROPERTIES_BITS);
    }

    public String getReasonString() {
        return properties.getReasonString();
    }

    public void setReasonString(String reasonString) {
        properties.setReasonString(reasonString);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }
}
