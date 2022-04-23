package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttVariableMessage<MqttConnAckVariableHeader> {

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(MqttFixedHeader.CONN_ACK_HEADER);
        setVariableHeader(mqttConnAckVariableHeader);
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();
        setVariableHeader(new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent));
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) {
        MqttConnAckVariableHeader variableHeader = getVariableHeader();
        writeBuffer.writeByte(getFixedHeaderByte1(fixedHeader));
        writeBuffer.writeByte((byte) 2);
        writeBuffer.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
        writeBuffer.writeByte(variableHeader.connectReturnCode().getCode());
    }

}
