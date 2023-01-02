package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.protocol.DecodeUnit;

import java.io.IOException;
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
public class MqttPacketIdentifierMessage extends MqttVariableMessage<MqttPacketIdVariableHeader> {

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader, int packetId) {
        super(mqttFixedHeader);
        MqttPacketIdVariableHeader header = new MqttPacketIdVariableHeader();
        header.setPacketId(packetId);
        setVariableHeader(header);
    }

    @Override
    public final void decodeVariableHeader(DecodeUnit unit, ByteBuffer buffer) {
        MqttPacketIdVariableHeader header = new MqttPacketIdVariableHeader();
        header.setPacketId(decodeMessageId(buffer));
        setVariableHeader(header);
    }


    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttPacketIdVariableHeader variableHeader = getVariableHeader();
        int variableHeaderBufferSize = 2; // variable part only has a message id
        mqttWriter.writeByte(getFixedHeaderByte1(fixedHeader));
        writeVariableLengthInt(mqttWriter, variableHeaderBufferSize);
        mqttWriter.writeShort((short) variableHeader.getPacketId());
    }
}
