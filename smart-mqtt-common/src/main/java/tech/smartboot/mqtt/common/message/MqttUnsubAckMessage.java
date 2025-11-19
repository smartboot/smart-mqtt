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
import tech.smartboot.mqtt.common.message.payload.Mqtt5UnsubAckPayload;
import tech.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    private Mqtt5UnsubAckPayload payload;

    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer, final MqttVersion version) {

    }

    public MqttUnsubAckMessage(MqttReasonVariableHeader variableHeader, Mqtt5UnsubAckPayload payload) {
        super(MqttFixedHeader.UNSUB_ACK_HEADER, variableHeader);
        this.payload = payload;
    }

    @Override
    protected Mqtt5UnsubAckPayload getPayload() {
        return payload;
    }
}
