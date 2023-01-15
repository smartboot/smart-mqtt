package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    private MqttSubAckPayload payload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubAckMessage(MqttReasonVariableHeader variableHeader) {
        super(MqttFixedHeader.SUB_ACK_HEADER, variableHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        MqttReasonVariableHeader header;
        if (version == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            properties.decode(buffer);
            header = new MqttReasonVariableHeader(packetId, properties);
        } else {
            header = new MqttReasonVariableHeader(packetId, null);
        }
        setVariableHeader(header);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        int payloadLength = fixedHeader.remainingLength() - getVariableHeaderLength();
        final List<Integer> grantedQos = new ArrayList<Integer>();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            grantedQos.add(qos);
        }
        buffer.limit(limit);
        payload = new MqttSubAckPayload(grantedQos);
    }


    public MqttSubAckPayload getPayload() {
        return this.payload;
    }

    public void setPayload(MqttSubAckPayload payload) {
        this.payload = payload;
    }
}
