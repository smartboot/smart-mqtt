package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.SubscribeProperties;
import org.smartboot.mqtt.common.message.properties.UserProperty;
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
public class MqttSubscribeMessage extends MqttVariableMessage<MqttSubscribeVariableHeader> {
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
            subscribeProperties = new SubscribeProperties();
            decodeSubscribeProperties(buffer, subscribeProperties);
        }
        MqttSubscribeVariableHeader header = new MqttSubscribeVariableHeader(packetId, subscribeProperties);
        setVariableHeader(header);
    }

    private void decodeSubscribeProperties(ByteBuffer buffer, SubscribeProperties subscribeProperties) {
        int remainingLength = decodeVariableByteInteger(buffer);
        if (remainingLength <= 0) {
            return;
        }
        int subscriptionIdentifier = -1;
        int position;
        while (remainingLength > 0) {
            position = buffer.position();
            switch (buffer.get()) {
                //订阅标识符
                case MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER:
                    //包含多个订阅标识符将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(subscriptionIdentifier == -1, "");
                    subscriptionIdentifier = buffer.getInt();
                    subscribeProperties.setSubscriptionIdentifier(subscriptionIdentifier);
                    //订阅标识符取值范围从1到268,435,455
                    ValidateUtils.isTrue(subscriptionIdentifier >= 1 && subscriptionIdentifier <= 268435455, "");
                    break;
                case MqttPropertyConstant.USER_PROPERTY:
                    String key = decodeString(buffer);
                    String value = decodeString(buffer);
                    subscribeProperties.getUserProperties().add(new UserProperty(key, value));
                    break;
            }
            remainingLength -= buffer.position() - position;
        }
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
        int payloadLength = fixedHeader.remainingLength() - getVariableHeaderLength();
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
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        int length = 2;
        List<byte[]> topicFilters = new ArrayList<>(mqttSubscribePayload.getTopicSubscriptions().size());
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            byte[] bytes = encodeUTF8(topicSubscription.getTopicFilter());
            topicFilters.add(bytes);
            length += 1 + bytes.length;
        }
        mqttWriter.writeByte(getFixedHeaderByte1(fixedHeader));
        mqttWriter.write(encodeMBI(length));
        mqttWriter.writeShort((short) getVariableHeader().getPacketId());
        int i = 0;
        for (MqttTopicSubscription topicSubscription : mqttSubscribePayload.getTopicSubscriptions()) {
            mqttWriter.write(topicFilters.get(i++));
            mqttWriter.writeByte((byte) topicSubscription.getQualityOfService().value());
        }
    }
}
