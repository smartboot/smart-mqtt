package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubRecMessage extends MqttPubQosMessage {
    public MqttPubRecMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubRecMessage(MqttPubQosVariableHeader variableHeader) {
        super(MqttFixedHeader.PUB_REC_HEADER, variableHeader);
    }

}
