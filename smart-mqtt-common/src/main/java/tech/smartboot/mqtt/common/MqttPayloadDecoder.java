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

import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.nio.ByteBuffer;

final class MqttPayloadDecoder implements Decoder {


    @Override
    public Decoder decode(ByteBuffer buffer, AbstractSession session) {
        int remainingLength = session.mqttMessage.getRemainingLength();
        ByteBuffer payloadBuffer;
        if (remainingLength > buffer.capacity()) {
            if (session.disposableBuffer == null) {
                payloadBuffer = session.disposableBuffer = ByteBuffer.allocate(remainingLength);
            } else {
                payloadBuffer = session.disposableBuffer;
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
            return this;
        }
        int p = payloadBuffer.position();
        session.mqttMessage.decodeVariableHeader(payloadBuffer, session.getMqttVersion());
        session.mqttMessage.decodePlayLoad(payloadBuffer);
        ValidateUtils.isTrue((payloadBuffer.position() - p) == remainingLength, "Payload size is wrong");
        session.disposableBuffer = null;
        return MqttProtocol.FINISH_DECODER;
    }

}
