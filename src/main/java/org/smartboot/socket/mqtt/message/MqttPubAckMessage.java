package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubAckMessage extends SingleByteFixedHeaderAndMessageIdMessage {
    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
    }
}
