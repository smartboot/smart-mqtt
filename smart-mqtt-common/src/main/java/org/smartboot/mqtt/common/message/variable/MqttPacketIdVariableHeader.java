package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttPacketIdVariableHeader<T extends AbstractProperties> extends MqttVariableHeader<T> {
    /**
     * 报文标识符
     */
    private final int packetId;

    public MqttPacketIdVariableHeader(int packetId, T properties) {
        super(properties);
        this.packetId = packetId;
    }


    public int getPacketId() {
        return packetId;
    }
}
