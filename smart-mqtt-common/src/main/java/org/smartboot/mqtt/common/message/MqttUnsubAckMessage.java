package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.payload.Mqtt5UnsubAckPayload;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;

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
    protected void decodeVariableHeader0(ByteBuffer buffer) {

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
