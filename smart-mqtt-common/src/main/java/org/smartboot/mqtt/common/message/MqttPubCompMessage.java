package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubCompMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader, int mqttPacketIdVariableHeader) {
        super(mqttFixedHeader, mqttPacketIdVariableHeader);
    }
}
