package org.smartboot.mqtt.common.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubAckMessage extends MqttPubQosMessage {

    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubAckMessage(MqttPubQosVariableHeader variableHeader) {
        super(MqttFixedHeader.PUB_ACK_HEADER);
        setVariableHeader(variableHeader);
    }

}
