package org.smartboot.mqtt.common.message.variable.properties;

import org.smartboot.mqtt.common.util.MqttPropertyConstant;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class SubscribeProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER_BIT | MqttPropertyConstant.USER_PROPERTY_BIT;
    /**
     * 订阅标识符
     */

    public SubscribeProperties() {
        super(PROPERTIES_BITS);
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
