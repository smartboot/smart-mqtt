package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubRelMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttPubRelMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubRelMessage(int packetId) {
        super(new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0), packetId);
    }
}
