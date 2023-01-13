package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer) {

    }

    public MqttUnsubAckMessage(MqttReasonVariableHeader variableHeader) {
        super(MqttFixedHeader.UNSUB_ACK_HEADER, variableHeader);
    }
}
