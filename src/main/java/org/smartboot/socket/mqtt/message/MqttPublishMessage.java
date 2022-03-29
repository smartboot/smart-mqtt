package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.util.MqttUtil;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.DecoderException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttMessage {
    private MqttPublishVariableHeader mqttPublishVariableHeader;

    private byte[] payload;

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader, byte[] payload) {
        super(mqttFixedHeader);
        this.mqttPublishVariableHeader = mqttPublishVariableHeader;
        this.payload = payload;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final String decodedTopic = decodeString(buffer);
        //PUBLISH 报文中的主题名不能包含通配符
        if (MqttUtil.containsTopicWildcards(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int messageId = -1;
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        if (mqttFixedHeader.getQosLevel().value() > 0) {
            messageId = decodeMessageId(buffer);
        }
        mqttPublishVariableHeader = new MqttPublishVariableHeader(decodedTopic, messageId);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        //QoS 设置为 0 的 PUBLISH 报文不能包含报文标识符
        int variableHeaderLength = mqttPublishVariableHeader.topicName().getBytes().length + (mqttFixedHeader.getQosLevel().value() > 0 ? 4 : 2);
        int remainingLength = mqttFixedHeader.remainingLength();
        int readLength = remainingLength - variableHeaderLength;
        payload = new byte[readLength];
        buffer.get(payload);
    }

    @Override
    public void writeTo(WriteBuffer page) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(mqttPublishVariableHeader.topicName());
        dos.writeShort(mqttPublishVariableHeader.packetId());
        dos.write(payload);
        dos.flush();
        byte[] varAndPayloadBytes = baos.toByteArray();
        baos.reset();
        dos.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        dos.write(encodeMBI(varAndPayloadBytes.length));
        dos.write(varAndPayloadBytes);
        dos.flush();
        byte[] data = baos.toByteArray();
        page.writeAndFlush(data);
    }

    public byte[] getPayload() {
        return payload;
    }

    public MqttPublishVariableHeader getMqttPublishVariableHeader() {
        return mqttPublishVariableHeader;
    }
}
