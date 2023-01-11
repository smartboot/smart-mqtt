package org.smartboot.mqtt.common.message.variable.properties;

import org.smartboot.mqtt.common.util.MqttPropertyConstant;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class ReasonProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = MqttPropertyConstant.REASON_STRING_BIT | MqttPropertyConstant.USER_PROPERTY_BIT;

    public ReasonProperties() {
        super(PROPERTIES_BITS);
    }

    public String getReasonString() {
        return properties.getReasonString();
    }

    public void setReasonString(String reasonString) {
        properties.setResponseTopic(reasonString);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }
}
