package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubRelMessage extends MqttPubQosMessage {
    public MqttPubRelMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubRelMessage(MqttPubQosVariableHeader variableHeader) {
        super(MqttFixedHeader.PUB_REL_HEADER, variableHeader);
    }
}
