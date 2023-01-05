package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.properties.SubscribeProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class MqttSubscribeVariableHeader extends MqttVariableHeader {
    /**
     * 报文标识符
     */
    private int packetId;
    private SubscribeProperties subscribeProperties;

    public MqttSubscribeVariableHeader(int packetId, SubscribeProperties subscribeProperties) {
        this.packetId = packetId;
        this.subscribeProperties = subscribeProperties;
    }

    public SubscribeProperties getSubscribeProperties() {
        return subscribeProperties;
    }

    public void setSubscribeProperties(SubscribeProperties subscribeProperties) {
        this.subscribeProperties = subscribeProperties;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }
}
