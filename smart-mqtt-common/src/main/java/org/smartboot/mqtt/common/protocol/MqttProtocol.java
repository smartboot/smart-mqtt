/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;

import static org.smartboot.mqtt.common.protocol.DecoderState.*;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttProtocol implements Protocol<MqttMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MqttProtocol.class);
    private final int maxBytesInMessage;

    public static final AttachKey<MqttVersion> MQTT_VERSION_ATTACH_KEY = AttachKey.valueOf("mqtt_version");
    private static final AttachKey<DecodeUnit> DECODE_UNIT_ATTACH_KEY = AttachKey.valueOf("decodeUnit");


    public MqttProtocol(int maxBytesInMessage) {
        this.maxBytesInMessage = maxBytesInMessage;
    }


    @Override
    public MqttMessage decode(ByteBuffer buffer, AioSession session) {
        Attachment attachment = session.getAttachment();
        DecodeUnit unit = attachment.get(DECODE_UNIT_ATTACH_KEY);
        if (unit == null) {
            unit = new DecodeUnit();
            unit.state = READ_FIXED_HEADER;
            attachment.put(DECODE_UNIT_ATTACH_KEY, unit);
        }

        switch (unit.state) {
            case READ_FIXED_HEADER: {
                if (buffer.remaining() < 2) {
                    break;
                }
                buffer.mark();
                short b1 = BufferUtils.readUnsignedByte(buffer);

                MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
//                    System.out.println("messageType:" + messageType);
                boolean dupFlag = (b1 & 0x08) == 0x08;
                int qosLevel = (b1 & 0x06) >> 1;
                boolean retain = (b1 & 0x01) != 0;

                int remainingLength = 0;
                int multiplier = 1;
                short digit;
                int loops = 0;
                do {
                    digit = BufferUtils.readUnsignedByte(buffer);
                    remainingLength += (digit & 127) * multiplier;
                    multiplier *= 128;
                    loops++;
                } while (buffer.hasRemaining() && (digit & 128) != 0 && loops < 4);

                //数据不足
                if (!buffer.hasRemaining() && (digit & 128) != 0) {
                    buffer.reset();
                    break;
                }
                // MQTT protocol limits Remaining Length to 4 bytes
                if (loops == 4 && (digit & 128) != 0) {
                    throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
                }
                buffer.mark();

                MqttFixedHeader mqttFixedHeader = MqttMessageFactory.newMqttFixedHeader(messageType, dupFlag, qosLevel, retain);
                unit.mqttMessage = MqttMessageFactory.newMessage(mqttFixedHeader);
                unit.mqttMessage.setRemainingLength(remainingLength);
                //非MqttConnectMessage对象为null,
                if (unit.mqttMessage.getVersion() == null) {
                    unit.mqttMessage.setVersion(attachment.get(MQTT_VERSION_ATTACH_KEY));
                }

                unit.state = READ_VARIABLE_HEADER;

            }
            case READ_VARIABLE_HEADER: {
                int remainingLength = unit.mqttMessage.getRemainingLength();
                if (remainingLength > maxBytesInMessage) {
                    throw new DecoderException("too large message: " + remainingLength + " bytes");
                }
                ByteBuffer payloadBuffer;
                if (remainingLength > buffer.capacity()) {
                    if (unit.disposableBuffer == null) {
                        payloadBuffer = unit.disposableBuffer = ByteBuffer.allocate(remainingLength);
                    } else {
                        payloadBuffer = unit.disposableBuffer;
                        payloadBuffer.compact();
                    }

                    if (payloadBuffer.remaining() >= buffer.remaining()) {
                        payloadBuffer.put(buffer);
                    } else {
                        int limit = buffer.limit();
                        buffer.limit(buffer.position() + payloadBuffer.remaining());
                        payloadBuffer.put(buffer);
                        buffer.limit(limit);
                    }
                    payloadBuffer.flip();
                } else {
                    payloadBuffer = buffer;
                }

                if (payloadBuffer.remaining() < remainingLength) {
                    break;
                }
                unit.mqttMessage.decodeVariableHeader(payloadBuffer);


                unit.state = READ_PAYLOAD;

                // fall through
            }

            case READ_PAYLOAD: {
                if (unit.disposableBuffer == null) {
                    unit.mqttMessage.decodePlayLoad(buffer);
                } else {
                    unit.mqttMessage.decodePlayLoad(unit.disposableBuffer);
                    ValidateUtils.isTrue(unit.disposableBuffer.remaining() == 0, "decode error");
                    unit.disposableBuffer = null;
                }
                unit.state = FINISH;
                break;
            }

            default:
                // Shouldn't reach here.
                throw new Error();
        }
        if (unit.state == FINISH) {
            MqttMessage mqttMessage = unit.mqttMessage;
            unit.state = READ_FIXED_HEADER;
            unit.mqttMessage = null;
            return mqttMessage;
        } else {
            return null;
        }
    }

}
