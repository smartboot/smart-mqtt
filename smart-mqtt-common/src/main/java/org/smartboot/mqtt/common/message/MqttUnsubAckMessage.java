package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends SingleByteFixedHeaderAndPacketIdMessage {
    public MqttUnsubAckMessage() {
        super(new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}
