package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubrecMessage extends SingleByteFixedHeaderAndMessageIdMessage {
    public MqttPubrecMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubrecMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
    }
}
