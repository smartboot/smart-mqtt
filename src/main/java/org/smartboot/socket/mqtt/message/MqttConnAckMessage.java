package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttConnectReturnCode;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttMessage {
    private MqttConnAckVariableHeader mqttConnAckVariableHeader;

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader, MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(mqttFixedHeader);
        this.mqttConnAckVariableHeader = mqttConnAckVariableHeader;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();
        mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeBuffer.writeByte((byte) 2);
        writeBuffer.writeByte((byte) (mqttConnAckVariableHeader.isSessionPresent() ? 0x01 : 0x00));
        writeBuffer.writeByte(mqttConnAckVariableHeader.connectReturnCode().getCode());
    }

    public MqttConnAckVariableHeader getMqttConnAckVariableHeader() {
        return mqttConnAckVariableHeader;
    }

    public void setMqttConnAckVariableHeader(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        this.mqttConnAckVariableHeader = mqttConnAckVariableHeader;
    }
}
