package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class SubscribeProperties {
    /**
     * 订阅标识符
     */
    private int subscriptionIdentifier;
    /**
     * 用户属性
     */
    private final List<UserProperty> userProperties = new ArrayList<>();

    public int getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    public void setSubscriptionIdentifier(int subscriptionIdentifier) {
        this.subscriptionIdentifier = subscriptionIdentifier;
    }

    public List<UserProperty> getUserProperties() {
        return userProperties;
    }
}
