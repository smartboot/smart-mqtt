package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SingleByteFixedHeaderAndPacketIdMessage extends MqttPacketIdentifierMessage {
    public SingleByteFixedHeaderAndPacketIdMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public SingleByteFixedHeaderAndPacketIdMessage(MqttFixedHeader mqttFixedHeader, int packageId) {
        super(mqttFixedHeader, packageId);
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int variableHeaderBufferSize = 2; // variable part only has a message id
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(writeBuffer, variableHeaderBufferSize);
        writeBuffer.writeShort((short) getPacketId());
    }
}
