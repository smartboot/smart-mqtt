package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPacketIdVariableHeader {
    private final int packetId;

    public static MqttPacketIdVariableHeader from(int packetId) {
        if (packetId < 1 || packetId > 0xffff) {
            throw new IllegalArgumentException("packetId: " + packetId + " (expected: 1 ~ 65535)");
        }
        return new MqttPacketIdVariableHeader(packetId);
    }

    private MqttPacketIdVariableHeader(int packetId) {
        this.packetId = packetId;
    }

    public int packetId() {
        return packetId;
    }

}
