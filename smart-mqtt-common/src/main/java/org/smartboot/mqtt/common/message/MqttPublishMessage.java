package org.smartboot.mqtt.common.message;

import org.apache.commons.collections4.CollectionUtils;
import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.MqttProperties;
import org.smartboot.mqtt.common.message.properties.PublishProperties;
import org.smartboot.mqtt.common.message.properties.UserProperty;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.*;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttVariableMessage<MqttPublishVariableHeader> {
    private static final int PROPERTIES_BITS = PAYLOAD_FORMAT_INDICATOR_BIT | MESSAGE_EXPIRY_INTERVAL_BIT | TOPIC_ALIAS_BIT | RESPONSE_TOPIC_BIT
            | CORRELATION_DATA_BIT | USER_PROPERTY_BIT | SUBSCRIPTION_IDENTIFIER_BIT | CONTENT_TYPE_BIT;
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
        final String decodedTopic = MqttCodecUtil.decodeString(buffer);
        //PUBLISH 报文中的主题名不能包含通配符
        if (MqttUtil.containsTopicWildcards(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int packetId = -1;
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        if (fixedHeader.getQosLevel().value() > 0) {
            packetId = decodeMessageId(buffer);
        }
        PublishProperties properties = null;
        if (version == MqttVersion.MQTT_5) {
            MqttProperties mqttProperties = new MqttProperties();
            mqttProperties.decode(buffer, PROPERTIES_BITS);
            properties = new PublishProperties(mqttProperties);
        }
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(packetId, decodedTopic, properties);
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
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttPublishVariableHeader variableHeader = getVariableHeader();
        byte[] topicBytes = encodeUTF8(variableHeader.getTopicName());
        boolean hasPacketId = fixedHeader.getQosLevel().value() > 0;
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));

        int length = topicBytes.length + (hasPacketId ? 2 : 0) + payload.length;
        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5) {
            //属性长度
            propertiesLength = preEncodeProperties(variableHeader.getPublishProperties());
            length += getVariableLengthInt(propertiesLength) + propertiesLength;
        }
        mqttWriter.write(encodeMBI(length));

        mqttWriter.write(topicBytes);
        if (hasPacketId) {
            mqttWriter.writeShort((short) variableHeader.getPacketId());
        }
        if (version == MqttVersion.MQTT_5) {
            //属性长度
            writeVariableLengthInt(mqttWriter, propertiesLength);
            writeProperties(mqttWriter, variableHeader.getPublishProperties());
        }
        mqttWriter.write(payload);
    }

    private int preEncodeProperties(PublishProperties properties) {
        if (properties == null) {
            return 0;
        }
        properties.decode();
        int length = 0;
        //载荷格式指示
        if (properties.getPayloadFormatIndicator() != -1) {
            length += 2;
        }
        //消息过期间隔
        if (properties.getMessageExpiryInterval() > 0) {
            length += 5;
        }
        //主题别名
        if (properties.getTopicAlias() > 0) {
            length += 2;
        }
        //响应主题
        if (properties.getResponseTopic() != null) {
            byte[] responseTopicBytes = encodeUTF8(properties.getResponseTopic());
            length += responseTopicBytes.length;
        }
        //对比数据
        if (properties.getCorrelationData() != null) {
            length += properties.getCorrelationData().length + 2;
        }
        //用户属性
        if (CollectionUtils.isNotEmpty(properties.getUserProperties())) {
            length += 1;
            for (UserProperty userProperty : properties.getUserProperties()) {
                userProperty.decode();
                length += userProperty.getKeyBytes().length + userProperty.getValueBytes().length;
            }
        }
        //订阅标识符
        if (properties.getSubscriptionIdentifier() > 0) {
            length += 2;
        }
        //内容类型
        if (properties.getContentType() != null) {
            byte[] contentTypeBytes = encodeUTF8(properties.getContentType());
            length += contentTypeBytes.length;
        }
        return length;
    }

    private void writeProperties(MqttWriter mqttWriter, PublishProperties properties) throws IOException {
        if (properties == null) {
            return;
        }
        //载荷格式指示
        if (properties.getPayloadFormatIndicator() != -1) {
            mqttWriter.writeByte(MqttPropertyConstant.PAYLOAD_FORMAT_INDICATOR);
            mqttWriter.writeByte(properties.getPayloadFormatIndicator());
        }
        //消息过期间隔
        if (properties.getMessageExpiryInterval() > 0) {
            mqttWriter.writeByte(MqttPropertyConstant.MESSAGE_EXPIRY_INTERVAL);
            mqttWriter.writeInt(properties.getMessageExpiryInterval());
        }
        //主题别名
        if (properties.getTopicAlias() > 0) {
            mqttWriter.writeByte(MqttPropertyConstant.TOPIC_ALIAS);
            mqttWriter.writeShort((short) properties.getTopicAlias());
        }
        //响应主题
        if (properties.getResponseTopicBytes() != null) {
            mqttWriter.writeByte(MqttPropertyConstant.RESPONSE_TOPIC);
            mqttWriter.write(properties.getResponseTopicBytes());
        }
        //对比数据
        if (properties.getCorrelationData() != null) {
            mqttWriter.writeByte(MqttPropertyConstant.CORRELATION_DATA);
            writeByteArray(mqttWriter, properties.getCorrelationData());
        }
        //用户属性
        if (CollectionUtils.isNotEmpty(properties.getUserProperties())) {
            mqttWriter.writeByte(MqttPropertyConstant.USER_PROPERTY);
            for (UserProperty userProperty : properties.getUserProperties()) {
                mqttWriter.write(userProperty.getKeyBytes());
                mqttWriter.write(userProperty.getValueBytes());
            }
        }
        //订阅标识符
        if (properties.getSubscriptionIdentifier() > 0) {
            mqttWriter.writeByte(MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER);
            writeVariableLengthInt(mqttWriter, properties.getSubscriptionIdentifier());
        }
        //内容类型
        if (properties.getContentTypeBytes() != null) {
            mqttWriter.writeByte(MqttPropertyConstant.CONTENT_TYPE);
            mqttWriter.write(properties.getContentTypeBytes());
        }
    }

    public byte[] getPayload() {
        return payload;
    }

}
