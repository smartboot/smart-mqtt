package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubCompMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }
    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader mqttPacketIdVariableHeader) {
        super(mqttFixedHeader, mqttPacketIdVariableHeader);
    }
}
