package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubCompMessage extends MqttPubQosMessage {
    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubCompMessage(MqttPubQosVariableHeader variableHeader) {
        super(MqttFixedHeader.PUB_COMP_HEADER, variableHeader);
    }
}
