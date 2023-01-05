package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttVariableMessage<MqttPublishVariableHeader> {
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
        final String decodedTopic = decodeString(buffer);
        //PUBLISH 报文中的主题名不能包含通配符
        if (MqttUtil.containsTopicWildcards(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int packetId = -1;
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        if (fixedHeader.getQosLevel().value() > 0) {
            packetId = decodeMessageId(buffer);
        }
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader();
        variableHeader.setPacketId(packetId);
        variableHeader.setTopicName(decodedTopic);
        setVariableHeader(variableHeader);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        //QoS 设置为 0 的 PUBLISH 报文不能包含报文标识符
        int variableHeaderLength = getVariableHeader().getTopicName().getBytes().length + (fixedHeader.getQosLevel().value() > 0 ? 4 : 2);
        int remainingLength = fixedHeader.remainingLength();
        int readLength = remainingLength - variableHeaderLength;
        payload = new byte[readLength];
        buffer.get(payload);
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttPublishVariableHeader variableHeader = getVariableHeader();
        byte[] topicBytes = encodeUTF8(variableHeader.getTopicName());
        boolean hasPacketId = fixedHeader.getQosLevel().value() > 0;
        mqttWriter.writeByte(getFixedHeaderByte1(fixedHeader));
        mqttWriter.write(encodeMBI(topicBytes.length + (hasPacketId ? 2 : 0) + payload.length));

        mqttWriter.write(topicBytes);
        if (hasPacketId) {
            mqttWriter.writeShort((short) variableHeader.getPacketId());
        }
        mqttWriter.write(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

}
