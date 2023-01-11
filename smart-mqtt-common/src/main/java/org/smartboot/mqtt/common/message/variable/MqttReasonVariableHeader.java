package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttReasonVariableHeader extends MqttPacketIdVariableHeader {

    private ReasonProperties properties;

    public MqttReasonVariableHeader(int packetId) {
        super(packetId);
    }

    public ReasonProperties getProperties() {
        return properties;
    }

    public void setProperties(ReasonProperties properties) {
        this.properties = properties;
    }
}
