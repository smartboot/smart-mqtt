package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;

/**
 * 包含报文标识符的消息类型
 * 很多控制报文的可变报头部分包含一个两字节的报文标识符字段。这些报文是 PUBLISH（QoS>0 时），
 * PUBACK，PUBREC，PUBREL，PUBCOMP，SUBSCRIBE, SUBACK，UNSUBSCIBE，
 * UNSUBACK。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPacketIdentifierMessage extends MqttMessage {
    /**
     * 报文标识符
     */
    protected int packetId;

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader, int packetId) {
        super(mqttFixedHeader);
        this.packetId = packetId;
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
        packetId = decodeMessageId(buffer);
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }
}
