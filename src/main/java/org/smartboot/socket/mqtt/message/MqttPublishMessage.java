package org.smartboot.socket.mqtt.message;

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

    private ByteBuffer payload;

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader, ByteBuffer payload) {
        super(mqttFixedHeader);
        this.mqttPublishVariableHeader = mqttPublishVariableHeader;
        this.payload = payload;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final String decodedTopic = decodeString(buffer);
        if (!isValidPublishTopicName(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int messageId = -1;
        if (mqttFixedHeader.getQosLevel().value() > 0) {
            messageId = decodeMessageId(buffer);
        }
        mqttPublishVariableHeader =
                new MqttPublishVariableHeader(decodedTopic, messageId);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        payload = ByteBuffer.allocate(buffer.remaining());
        payload.put(buffer);
        payload.flip();
    }

    @Override
    public void writeTo(WriteBuffer page) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(mqttPublishVariableHeader.topicName());
        dos.writeShort(mqttPublishVariableHeader.packetId());
        dos.write(payload.array());
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

    public ByteBuffer getPayload() {
        return payload;
    }

    public MqttPublishVariableHeader getMqttPublishVariableHeader() {
        return mqttPublishVariableHeader;
    }
}
