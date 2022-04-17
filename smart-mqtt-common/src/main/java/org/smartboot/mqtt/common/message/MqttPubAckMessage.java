package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubAckMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubAckMessage(int packetId) {
        super(MqttFixedHeader.PUB_ACK_HEADER, packetId);
    }
}
