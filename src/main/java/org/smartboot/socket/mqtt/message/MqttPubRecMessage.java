package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubRecMessage extends SingleByteFixedHeaderAndMessageIdMessage {
    public MqttPubRecMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubRecMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
    }

    public MqttPubRecMessage() {
        super(new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}
