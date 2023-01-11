package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class MqttSubscribeVariableHeader extends MqttPacketIdVariableHeader {

    private SubscribeProperties subscribeProperties;

    public MqttSubscribeVariableHeader(int packetId, SubscribeProperties subscribeProperties) {
        super(packetId);
        this.subscribeProperties = subscribeProperties;
    }

    public SubscribeProperties getSubscribeProperties() {
        return subscribeProperties;
    }

    public void setSubscribeProperties(SubscribeProperties subscribeProperties) {
        this.subscribeProperties = subscribeProperties;
    }

}
