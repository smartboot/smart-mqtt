package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }
}
