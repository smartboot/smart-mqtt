package org.smartboot.mqtt.common.message;

import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MqttPacketIdentifierMessage {
    private MqttSubAckPayload mqttSubAckPayload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubAckMessage(int packetId) {
        super(MqttFixedHeader.SUB_ACK_HEADER, packetId);
    }


    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<Integer> grantedQos = new ArrayList<Integer>();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + mqttFixedHeader.remainingLength() - PACKET_LENGTH);
        while (buffer.hasRemaining()) {
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            grantedQos.add(qos);
        }
        buffer.limit(limit);
        mqttSubAckPayload = new MqttSubAckPayload(grantedQos);
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = mqttSubAckPayload.grantedQoSLevels().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(writeBuffer, variablePartSize);
        writeBuffer.writeShort((short) getPacketId());
        for (int qos : mqttSubAckPayload.grantedQoSLevels()) {
            writeBuffer.writeByte((byte) qos);
        }
    }

    public MqttSubAckPayload getMqttSubAckPayload() {
        return this.mqttSubAckPayload;
    }

    public void setMqttSubAckPayload(MqttSubAckPayload mqttSubAckPayload) {
        this.mqttSubAckPayload = mqttSubAckPayload;
    }
}
