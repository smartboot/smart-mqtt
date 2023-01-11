package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttSubscribePayload;
import org.smartboot.mqtt.common.message.variable.MqttSubscribeVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.MqttProperties;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubscribeMessage extends MqttPacketIdentifierMessage<MqttSubscribeVariableHeader> {
    private static final int PROPERTIES_BITS = MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER_BIT | MqttPropertyConstant.USER_PROPERTY_BIT;
    private MqttSubscribePayload mqttSubscribePayload;

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttSubscribeVariableHeader variableHeader, MqttSubscribePayload mqttSubscribePayload) {
        super(mqttFixedHeader);
        setVariableHeader(variableHeader);
        this.mqttSubscribePayload = mqttSubscribePayload;
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        SubscribeProperties subscribeProperties = null;
        if (version == MqttVersion.MQTT_5) {
            MqttProperties mqttProperties = new MqttProperties();
            mqttProperties.decode(buffer, PROPERTIES_BITS);
            subscribeProperties = new SubscribeProperties(mqttProperties);
        }
        MqttSubscribeVariableHeader header = new MqttSubscribeVariableHeader(packetId, subscribeProperties);
        setVariableHeader(header);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
        int payloadLength = fixedHeader.remainingLength() - getVariableHeaderLength();
        ValidateUtils.isTrue(buffer.remaining() >= payloadLength, "数据不足");
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = MqttCodecUtil.decodeUTF8(buffer);
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
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        int length = 2;
        List<byte[]> topicFilters = new ArrayList<>(mqttSubscribePayload.getTopicSubscriptions().size());
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            byte[] bytes = MqttCodecUtil.encodeUTF8(topicSubscription.getTopicFilter());
            topicFilters.add(bytes);
            length += 1 + bytes.length;
        }
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));
        MqttCodecUtil.writeVariableLengthInt(mqttWriter, length);
        mqttWriter.writeShort((short) getVariableHeader().getPacketId());
        int i = 0;
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            mqttWriter.write(topicFilters.get(i++));
            mqttWriter.writeByte((byte) topicSubscription.getQualityOfService().value());
        }
    }
}
