package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SingleByteFixedHeaderAndMessageIdMessage extends MessageIdVariableHeaderMessage {
    public SingleByteFixedHeaderAndMessageIdMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public SingleByteFixedHeaderAndMessageIdMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int msgId = mqttMessageIdVariableHeader.messageId();

        int variableHeaderBufferSize = 2; // variable part only has a message id
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(writeBuffer, variableHeaderBufferSize);
        writeBuffer.writeShort((short) msgId);
    }
}
