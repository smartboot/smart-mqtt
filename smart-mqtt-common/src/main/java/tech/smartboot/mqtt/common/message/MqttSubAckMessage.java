/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message;

import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.payload.MqttSubAckPayload;
import tech.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    private MqttSubAckPayload payload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubAckMessage(MqttReasonVariableHeader variableHeader) {
        super(MqttFixedHeader.SUB_ACK_HEADER, variableHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer, final MqttVersion version) {
        int packetId = decodeMessageId(buffer);
        MqttReasonVariableHeader header;
        if (version == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            properties.decode(buffer);
            header = new MqttReasonVariableHeader(packetId, properties);
        } else {
            header = new MqttReasonVariableHeader(packetId, null);
        }
        setVariableHeader(header);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        int payloadLength = getRemainingLength() - getVariableHeaderLength();
        final List<Integer> grantedQos = new ArrayList<Integer>();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            int qos = buffer.get() & 0x03;
            grantedQos.add(qos);
        }
        buffer.limit(limit);
        payload = new MqttSubAckPayload(grantedQos);
    }


    public MqttSubAckPayload getPayload() {
        return this.payload;
    }

    public void setPayload(MqttSubAckPayload payload) {
        this.payload = payload;
    }
}
