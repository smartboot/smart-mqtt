package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubRelMessage extends MqttPacketIdentifierMessage {
    public MqttPubRelMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubRelMessage(int packetId) {
        super(MqttFixedHeader.PUB_REL_HEADER, packetId);
    }
}
