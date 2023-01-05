package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.PublishProperties;
import org.smartboot.mqtt.common.message.properties.UserProperty;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
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
        PublishProperties publishProperties = null;
        if (version == MqttVersion.MQTT_5) {
            publishProperties = new PublishProperties();
            decodePublishProperties(buffer, publishProperties);
        }
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(packetId, decodedTopic, publishProperties);
        setVariableHeader(variableHeader);
    }

    private void decodePublishProperties(ByteBuffer buffer, PublishProperties publishProperties) {
        int remainingLength = decodeVariableByteInteger(buffer);
        if (remainingLength == 0) {
            return;
        }

        int topicAlias = -1;
        int subscriptionIdentifier = -1;
        int messageExpiryInterval = -1;
        byte payloadFormatIndicator = -1;
        int position;
        while (remainingLength > 0) {
            position = buffer.position();
            switch (buffer.get()) {
                //载荷格式指示
                case MqttPropertyConstant.PAYLOAD_FORMAT_INDICATOR:
                    //包含多个载荷格式指示（Payload Format Indicator）将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(payloadFormatIndicator == -1, "");
                    payloadFormatIndicator = buffer.get();
                    publishProperties.setPayloadFormatIndicator(payloadFormatIndicator);
                    ValidateUtils.isTrue(payloadFormatIndicator == 0 || payloadFormatIndicator == 1, "");
                    break;
                //消息过期间隔
                case MqttPropertyConstant.MESSAGE_EXPIRY_INTERVAL:
                    //包含多个消息过期间隔将导致协议错误（Protocol Error）
                    ValidateUtils.isTrue(messageExpiryInterval == -1, "");
                    messageExpiryInterval = buffer.getInt();
                    publishProperties.setMessageExpiryInterval(messageExpiryInterval);
                    ValidateUtils.isTrue(messageExpiryInterval > 0, "");
                    break;
                //最大报文长度
                case MqttPropertyConstant.TOPIC_ALIAS:
                    //包含多个主题别名值将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(topicAlias == -1, "");
                    topicAlias = decodeMsbLsb(buffer);
                    publishProperties.setTopicAlias(decodeMsbLsb(buffer));
                    //todo
                    break;
                //响应主题
                case MqttPropertyConstant.RESPONSE_TOPIC:
                    ValidateUtils.isTrue(publishProperties.getResponseTopic() == null, "");
                    publishProperties.setResponseTopic(decodeString(buffer));
                    break;
                //请求响应信息
                case MqttPropertyConstant.CORRELATION_DATA:
                    ValidateUtils.isTrue(publishProperties.getCorrelationData() == null, "");
                    publishProperties.setCorrelationData(decodeByteArray(buffer));
                    break;
                //用户属性
                case MqttPropertyConstant.USER_PROPERTY:
                    String key = decodeString(buffer);
                    String value = decodeString(buffer);
                    publishProperties.getUserProperties().add(new UserProperty(key, value));
                    break;
                //订阅标识符
                case MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER:
                    //包含多个订阅标识符将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(subscriptionIdentifier == -1, "");
                    subscriptionIdentifier = buffer.getInt();
                    publishProperties.setSubscriptionIdentifier(subscriptionIdentifier);
                    //订阅标识符取值范围从1到268,435,455
                    ValidateUtils.isTrue(subscriptionIdentifier >= 1 && subscriptionIdentifier <= 268435455, "");
                    break;
                //内容类型
                case MqttPropertyConstant.CONTENT_TYPE:
                    ValidateUtils.isTrue(publishProperties.getContentType() == null, "");
                    publishProperties.setContentType(decodeString(buffer));
                    break;
            }
            remainingLength -= buffer.position() - position;
        }
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
