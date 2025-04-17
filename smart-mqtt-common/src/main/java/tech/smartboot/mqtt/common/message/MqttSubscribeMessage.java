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

import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.payload.MqttSubscribePayload;
import tech.smartboot.mqtt.common.message.variable.MqttSubscribeVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubscribeMessage extends MqttPacketIdentifierMessage<MqttSubscribeVariableHeader> {

    private MqttSubscribePayload payload;

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttSubscribeVariableHeader variableHeader,
                                MqttSubscribePayload payload) {
        super(mqttFixedHeader);
        setVariableHeader(variableHeader);
        this.payload = payload;
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        MqttSubscribeVariableHeader header;
        if (version == MqttVersion.MQTT_5) {
            SubscribeProperties properties = new SubscribeProperties();
            properties.decode(buffer);
            header = new MqttSubscribeVariableHeader(packetId, properties);
        } else {
            header = new MqttSubscribeVariableHeader(packetId, null);
        }
        setVariableHeader(header);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
        int payloadLength = getRemainingLength() - getVariableHeaderLength();
        ValidateUtils.isTrue(buffer.remaining() >= payloadLength, "数据不足");
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = MqttCodecUtil.decodeUTF8(buffer);
            int qos = buffer.get() & 0x03;
            MqttTopicSubscription subscription = new MqttTopicSubscription();
            subscription.setTopicFilter(decodedTopicName);
            subscription.setQualityOfService(MqttQoS.valueOf(qos));
            subscribeTopics.add(subscription);
        }
        buffer.limit(limit);
        this.payload = new MqttSubscribePayload();
        payload.setTopicSubscriptions(subscribeTopics);
    }

    public MqttSubscribePayload getPayload() {
        return payload;
    }

}
