package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class MqttSubscribeVariableHeader extends MqttPacketIdVariableHeader {

    private SubscribeProperties properties;

    public MqttSubscribeVariableHeader(int packetId) {
        super(packetId);
    }

    public SubscribeProperties getProperties() {
        return properties;
    }

    public void setProperties(SubscribeProperties properties) {
        this.properties = properties;
    }

}
