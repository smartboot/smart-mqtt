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
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
        int payloadLength = fixedHeader.remainingLength() - PACKET_LENGTH;
        ValidateUtils.isTrue(buffer.remaining() >= payloadLength, "数据不足");
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            MqttTopicSubscription subscription = new MqttTopicSubscription();
            subscription.setTopicFilter(decodedTopicName);
            subscription.setQualityOfService(MqttQoS.valueOf(qos));
            subscribeTopics.add(subscription);
        }
        buffer.limit(limit);
        this.mqttSubscribePayload = new MqttSubscribePayload();
        mqttSubscribePayload.setTopicSubscriptions(subscribeTopics);
    }

    public MqttSubscribePayload getMqttSubscribePayload() {
        return mqttSubscribePayload;
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int length = 2;
        List<byte[]> topicFilters = new ArrayList<>(mqttSubscribePayload.getTopicSubscriptions().size());
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            byte[] bytes = encodeUTF8(topicSubscription.getTopicFilter());
            topicFilters.add(bytes);
            length += 1 + bytes.length;
        }
        writeBuffer.writeByte(getFixedHeaderByte1(fixedHeader));
        writeBuffer.write(encodeMBI(length));
        writeBuffer.writeShort((short) getVariableHeader().getPacketId());
        int i = 0;
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            writeBuffer.write(topicFilters.get(i++));
            writeBuffer.writeByte((byte) topicSubscription.getQualityOfService().value());
        }
    }
}
