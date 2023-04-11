/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
