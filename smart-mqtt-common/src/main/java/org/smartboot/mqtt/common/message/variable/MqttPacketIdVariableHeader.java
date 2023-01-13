package org.smartboot.mqtt.common.message.variable;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttPacketIdVariableHeader extends MqttVariableHeader {
    /**
     * 报文标识符
     */
    private final int packetId;

    public MqttPacketIdVariableHeader(int packetId) {
        this.packetId = packetId;
    }


    public int getPacketId() {
        return packetId;
    }
}
