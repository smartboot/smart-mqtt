package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

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
        int payloadLength = mqttFixedHeader.remainingLength() - PACKET_LENGTH;
        ValidateUtils.isTrue(buffer.remaining() >= payloadLength, "数据不足");
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            subscribeTopics.add(new MqttTopicSubscription(decodedTopicName, MqttQoS.valueOf(qos)));
        }
        buffer.limit(limit);
        this.mqttSubscribePayload = new MqttSubscribePayload(subscribeTopics);
    }

    public MqttSubscribePayload getMqttSubscribePayload() {
        return mqttSubscribePayload;
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int length = 2;
        List<byte[]> topicFilters = new ArrayList<>(mqttSubscribePayload.topicSubscriptions().size());
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.topicSubscriptions()) {
            byte[] bytes = encodeUTF8(topicSubscription.topicFilter());
            topicFilters.add(bytes);
            length += 1 + bytes.length;
        }
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeBuffer.write(encodeMBI(length));
        writeBuffer.writeShort((short) packetId);
        int i = 0;
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.topicSubscriptions()) {
            writeBuffer.write(topicFilters.get(i++));
            writeBuffer.writeByte(topicSubscription.qualityOfService().value());
        }
    }
}
