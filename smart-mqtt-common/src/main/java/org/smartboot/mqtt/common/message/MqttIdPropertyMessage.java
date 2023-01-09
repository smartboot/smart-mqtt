package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 包含PacketId和Properties的消息类型
 *
 * @author cea
 * @version V1.0 , 2018/4/22
 */
public class MqttIdPropertyMessage extends MqttVariableMessage<MqttPubReplyVariableHeader> {

    public MqttIdPropertyMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttIdPropertyMessage(MqttFixedHeader mqttFixedHeader, int packetId) {
        this(mqttFixedHeader, packetId, (byte) 0, null);
    }

    public MqttIdPropertyMessage(MqttFixedHeader mqttFixedHeader, int packetId, byte reasonCode, MqttProperties mqttProperties) {
        super(mqttFixedHeader);
        setVariableHeader(new MqttPubReplyVariableHeader(packetId, reasonCode, mqttProperties));
    }

    @Override
    public final void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = buffer.getShort();
        MqttPubReplyVariableHeader header;
        if (version == MqttVersion.MQTT_5) {
            byte reasonCode = buffer.get();
            byte propertyLen = buffer.get();
            header = new MqttPubReplyVariableHeader(packetId, reasonCode, null);
        } else {
            header = new MqttPubReplyVariableHeader(packetId, (byte) 0, null);
        }
        setVariableHeader(header);
    }


    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttPubReplyVariableHeader variableHeader = getVariableHeader();
        int variableHeaderBufferSize = 2; // variable part only has a message id
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));
        MqttCodecUtil.writeVariableLengthInt(mqttWriter, variableHeaderBufferSize);
        mqttWriter.writeShort((short) variableHeader.getPacketId());
        mqttWriter.writeByte(variableHeader.getReasonCode());
    }
}
