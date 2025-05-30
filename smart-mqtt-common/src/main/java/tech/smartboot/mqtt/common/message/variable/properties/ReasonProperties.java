/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message.variable.properties;

import tech.smartboot.mqtt.common.util.MqttPropertyConstant;

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
