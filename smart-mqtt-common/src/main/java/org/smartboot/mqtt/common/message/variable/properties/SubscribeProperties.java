package org.smartboot.mqtt.common.message.variable.properties;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class SubscribeProperties {
    /**
     * 订阅标识符
     */
    private final MqttProperties properties;

    public SubscribeProperties(MqttProperties properties) {
        this.properties = properties;
    }

    public int getSubscriptionIdentifier() {
        return properties.getSubscriptionIdentifier();
    }

    public void setSubscriptionIdentifier(int subscriptionIdentifier) {
        properties.setSubscriptionIdentifier(subscriptionIdentifier);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }
}
