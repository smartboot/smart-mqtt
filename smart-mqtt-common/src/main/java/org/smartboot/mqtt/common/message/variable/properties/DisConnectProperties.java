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

import java.util.List;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.REASON_STRING_BIT;
import static org.smartboot.mqtt.common.util.MqttPropertyConstant.SERVER_REFERENCE_BIT;
import static org.smartboot.mqtt.common.util.MqttPropertyConstant.SESSION_EXPIRY_INTERVAL_BIT;
import static org.smartboot.mqtt.common.util.MqttPropertyConstant.USER_PROPERTY_BIT;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class DisConnectProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = SESSION_EXPIRY_INTERVAL_BIT | REASON_STRING_BIT | USER_PROPERTY_BIT | SERVER_REFERENCE_BIT;

    public DisConnectProperties() {
        super(PROPERTIES_BITS);
    }

    public int getSessionExpiryInterval() {
        return properties.getSessionExpiryInterval();
    }

    public void setSessionExpiryInterval(int sessionExpiryInterval) {
        properties.setSessionExpiryInterval(sessionExpiryInterval);
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


    public String getServerReference() {
        return properties.getServerReference();
    }

    public void setServerReference(String serverReference) {
        properties.setServerReference(serverReference);
    }

}
