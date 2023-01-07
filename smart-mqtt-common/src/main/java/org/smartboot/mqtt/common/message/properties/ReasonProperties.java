package org.smartboot.mqtt.common.message.properties;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class ReasonProperties {

    private final MqttProperties properties;

    public ReasonProperties(MqttProperties properties) {
        this.properties = properties;
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
