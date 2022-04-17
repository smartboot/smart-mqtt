package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPingRespMessage extends OnlyFixedHeaderMessage {
    public MqttPingRespMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPingRespMessage() {
        super(MqttFixedHeader.PING_RESP_HEADER);
    }
}