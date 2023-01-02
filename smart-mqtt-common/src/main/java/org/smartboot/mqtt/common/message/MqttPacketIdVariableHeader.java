package org.smartboot.mqtt.common.message;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public class MqttPacketIdVariableHeader extends MqttVariableHeader {
    /**
     * 报文标识符
     */
    private int packetId;

    public MqttPacketIdVariableHeader() {
    }

    public MqttPacketIdVariableHeader(int packetId) {
        this.packetId = packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public int getPacketId() {
        return packetId;
    }
}
