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
import tech.smartboot.mqtt.common.message.payload.MqttUnsubscribePayload;
import tech.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import tech.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 取消订阅
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubscribeMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    /**
     * UNSUBSCRIBE 报文的有效载荷包含客户端想要取消订阅的主题过滤器列表。
     */
    private MqttUnsubscribePayload mqttUnsubscribePayload;

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer, final MqttVersion version) {
        int packetId = decodeMessageId(buffer);
        MqttPubQosVariableHeader header;
        if (version == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            properties.decode(buffer);
            header = new MqttPubQosVariableHeader(packetId, properties);
        } else {
            header = new MqttPubQosVariableHeader(packetId, null);
        }
        setVariableHeader(header);
    }

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttReasonVariableHeader variableHeader, MqttUnsubscribePayload mqttUnsubscribePayload) {
        super(mqttFixedHeader, variableHeader);
        this.mqttUnsubscribePayload = mqttUnsubscribePayload;
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<String> unsubscribeTopics = new ArrayList<String>();
        int payloadLength = getRemainingLength() - getVariableHeaderLength();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + payloadLength);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = MqttCodecUtil.decodeUTF8(buffer);
            unsubscribeTopics.add(decodedTopicName);
        }
        buffer.limit(limit);
        mqttUnsubscribePayload = new MqttUnsubscribePayload(unsubscribeTopics);
    }

    public MqttUnsubscribePayload getMqttUnsubscribePayload() {
        return mqttUnsubscribePayload;
    }
}
