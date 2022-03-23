package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubscribeMessage extends MqttPacketIdentifierMessage {

    private MqttSubscribePayload mqttSubscribePayload;

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, int packageId, MqttSubscribePayload mqttSubscribePayload) {
        super(mqttFixedHeader, packageId);
        this.mqttSubscribePayload = mqttSubscribePayload;
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<MqttTopicSubscription>();
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            subscribeTopics.add(new MqttTopicSubscription(decodedTopicName, MqttQoS.valueOf(qos)));
        }
        this.mqttSubscribePayload = new MqttSubscribePayload(subscribeTopics);
    }

    public MqttSubscribePayload getMqttSubscribePayload() {
        return mqttSubscribePayload;
    }

    @Override
    public void writeTo(WriteBuffer page) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(getPacketId());
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.topicSubscriptions()) {
            dos.writeUTF(topicSubscription.topicName());
            dos.writeByte(topicSubscription.qualityOfService().value());
        }
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
}
