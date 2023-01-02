package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubCompMessage extends MqttPacketIdentifierMessage {
    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubCompMessage(int packetId) {
        super(MqttFixedHeader.PUB_COMP_HEADER, packetId);
    }
}
