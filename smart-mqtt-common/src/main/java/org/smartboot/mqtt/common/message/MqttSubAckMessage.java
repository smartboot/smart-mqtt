package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    private MqttSubAckPayload mqttSubAckPayload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubAckMessage(MqttReasonVariableHeader variableHeader) {
        super(MqttFixedHeader.SUB_ACK_HEADER, variableHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer) {

    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<Integer> grantedQos = new ArrayList<Integer>();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + fixedHeader.remainingLength() - PACKET_LENGTH);
        while (buffer.hasRemaining()) {
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            grantedQos.add(qos);
        }
        buffer.limit(limit);
        mqttSubAckPayload = new MqttSubAckPayload(grantedQos);
    }

    @Override
    public void writeWithoutFixedHeader(MqttWriter mqttWriter) throws IOException {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = mqttSubAckPayload.grantedQoSLevels().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;

        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5) {
            propertiesLength = variableHeader.getProperties().preEncode();
            variablePartSize += MqttCodecUtil.getVariableLengthInt(propertiesLength) + propertiesLength;
        }

        MqttCodecUtil.writeVariableLengthInt(mqttWriter, variablePartSize);
        mqttWriter.writeShort((short) getVariableHeader().getPacketId());
        if (version == MqttVersion.MQTT_5) {
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            variableHeader.getProperties().writeTo(mqttWriter);
        }
        for (int qos : mqttSubAckPayload.grantedQoSLevels()) {
            mqttWriter.writeByte((byte) qos);
        }
    }

    public MqttSubAckPayload getMqttSubAckPayload() {
        return this.mqttSubAckPayload;
    }

    public void setMqttSubAckPayload(MqttSubAckPayload mqttSubAckPayload) {
        this.mqttSubAckPayload = mqttSubAckPayload;
    }
}
