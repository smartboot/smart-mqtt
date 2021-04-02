package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class PacketIdVariableHeaderMessage extends MqttMessage {
    protected MqttPacketIdVariableHeader mqttPacketIdVariableHeader;

    public PacketIdVariableHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public PacketIdVariableHeaderMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader mqttPacketIdVariableHeader) {
        super(mqttFixedHeader);
        this.mqttPacketIdVariableHeader = mqttPacketIdVariableHeader;
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
        final int messageId = decodeMessageId(buffer);
        mqttPacketIdVariableHeader = MqttPacketIdVariableHeader.from(messageId);
    }

    public void setMqttMessageIdVariableHeader(MqttPacketIdVariableHeader mqttPacketIdVariableHeader) {
        this.mqttPacketIdVariableHeader = mqttPacketIdVariableHeader;
    }

    public MqttPacketIdVariableHeader getMqttMessageIdVariableHeader() {
        return mqttPacketIdVariableHeader;
    }
}
