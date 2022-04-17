package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttUnsubAckMessage(int packetId) {
        super(MqttFixedHeader.UNSUB_ACK_HEADER, packetId);
    }
}
