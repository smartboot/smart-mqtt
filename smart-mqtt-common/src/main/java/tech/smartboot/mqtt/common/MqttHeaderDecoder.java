/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import org.smartboot.socket.DecoderException;
import tech.smartboot.mqtt.common.enums.MqttMessageType;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttDisconnectMessage;
import tech.smartboot.mqtt.common.message.MqttFixedHeader;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPingReqMessage;
import tech.smartboot.mqtt.common.message.MqttPingRespMessage;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubCompMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubAckMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;

import java.nio.ByteBuffer;

final class MqttHeaderDecoder implements Decoder {
    private final MqttPayloadDecoder mqttPayloadDecoder = new MqttPayloadDecoder();
    private final int maxBytesInMessage;

    public MqttHeaderDecoder(int maxBytesInMessage) {
        this.maxBytesInMessage = maxBytesInMessage;
    }

    @Override
    public Decoder decode(ByteBuffer buffer, AbstractSession session) {
        if (buffer.remaining() < 2) {
            return this;
        }
        buffer.mark();
        final byte b1 = buffer.get();
        MqttMessageType messageType = MqttMessageType.valueOf((b1 & 0xff) >> 4);

        //解析MQTT消息长度
        int remainingLength = 0;
        int multiplier = 1;
        int loops = 0;
        do {
            byte digit = buffer.get();
            if ((digit & 128) == 0) {
                remainingLength += digit * multiplier;
                break;
            } else {
                remainingLength += (digit & 127) * multiplier;
            }
            // MQTT protocol limits Remaining Length to 4 bytes
            if (++loops == 4) {
                throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
            }
            //数据不足
            if (!buffer.hasRemaining()) {
                buffer.reset();
                return this;
            }
            multiplier <<= 7;
        } while (true);
        buffer.mark();

        if (remainingLength > maxBytesInMessage) {
            throw new DecoderException("too large message: " + remainingLength + " bytes");
        }

        boolean dupFlag = (b1 & 0x08) == 0x08;
        int qosLevel = (b1 & 0x06) >> 1;
        boolean retain = (b1 & 0x01) != 0;


        MqttFixedHeader mqttFixedHeader = MqttFixedHeader.getInstance(messageType, dupFlag, qosLevel, retain);
        session.mqttMessage = newMessage(mqttFixedHeader);
        session.mqttMessage.setRemainingLength(remainingLength);
        //非MqttConnectMessage对象为null,
        if (session.mqttMessage.getVersion() == null) {
            session.mqttMessage.setVersion(session.getMqttVersion());
        }
        return mqttPayloadDecoder.decode(buffer, session);
    }

    private MqttMessage newMessage(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.getMessageType()) {
            case CONNECT:
                return new MqttConnectMessage(mqttFixedHeader);

            case CONNACK:
                return new MqttConnAckMessage(mqttFixedHeader);

            case SUBSCRIBE:
                return new MqttSubscribeMessage(mqttFixedHeader);

            case SUBACK:
                return new MqttSubAckMessage(mqttFixedHeader);

            case UNSUBACK:
                return new MqttUnsubAckMessage(mqttFixedHeader);

            case UNSUBSCRIBE:
                return new MqttUnsubscribeMessage(mqttFixedHeader);

            case PUBLISH:
                return new MqttPublishMessage(mqttFixedHeader);

            case PUBACK:
                return new MqttPubAckMessage(mqttFixedHeader);
            case PUBREC:
                return new MqttPubRecMessage(mqttFixedHeader);
            case PUBREL:
                return new MqttPubRelMessage(mqttFixedHeader);
            case PUBCOMP:
                return new MqttPubCompMessage(mqttFixedHeader);

            case PINGREQ:
                return new MqttPingReqMessage(mqttFixedHeader);
            case PINGRESP:
                return new MqttPingRespMessage(mqttFixedHeader);
            case DISCONNECT:
                return new MqttDisconnectMessage();

            default:
                throw new IllegalArgumentException("unknown message type: " + mqttFixedHeader.getMessageType());
        }
    }
}
