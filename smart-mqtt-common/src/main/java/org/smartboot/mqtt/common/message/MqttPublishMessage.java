package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttVariableMessage<MqttPublishVariableHeader> {
    private static final byte[] EMPTY_BYTES = new byte[0];
    private byte[] payload;

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader, byte[] payload) {
        super(mqttFixedHeader);
        setVariableHeader(mqttPublishVariableHeader);
        this.payload = payload;
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        final String decodedTopic = MqttCodecUtil.decodeUTF8(buffer);
        //PUBLISH 报文中的主题名不能包含通配符
        if (MqttUtil.containsTopicWildcards(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int packetId = -1;
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        if (fixedHeader.getQosLevel().value() > 0) {
            packetId = decodeMessageId(buffer);
        }
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(packetId, decodedTopic);
        if (version == MqttVersion.MQTT_5) {
            PublishProperties properties = new PublishProperties();
            properties.decode(buffer);
            variableHeader.setProperties(properties);
        }

        setVariableHeader(variableHeader);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        int remainingLength = fixedHeader.remainingLength();
        int readLength = remainingLength - getVariableHeaderLength();
        if (readLength == 0) {
            payload = EMPTY_BYTES;
        } else {
            payload = new byte[readLength];
            buffer.get(payload);
        }
    }

    @Override
    public void writeWithoutFixedHeader(MqttWriter mqttWriter) throws IOException {
        MqttPublishVariableHeader variableHeader = getVariableHeader();
        byte[] topicBytes = MqttCodecUtil.encodeUTF8(variableHeader.getTopicName());
        boolean hasPacketId = fixedHeader.getQosLevel().value() > 0;


        int length = topicBytes.length + (hasPacketId ? 2 : 0) + payload.length;
        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5) {
            //属性长度
            propertiesLength = variableHeader.getProperties().preEncode();
            length += MqttCodecUtil.getVariableLengthInt(propertiesLength) + propertiesLength;
        }
        MqttCodecUtil.writeVariableLengthInt(mqttWriter, length);

        mqttWriter.write(topicBytes);
        if (hasPacketId) {
            mqttWriter.writeShort((short) variableHeader.getPacketId());
        }
        if (version == MqttVersion.MQTT_5) {
            //属性长度
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            variableHeader.getProperties().writeTo(mqttWriter);
        }
        mqttWriter.write(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

}
