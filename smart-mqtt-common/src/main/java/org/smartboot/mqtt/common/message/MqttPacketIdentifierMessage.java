package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

/**
 * 包含报文标识符的消息类型
 * 很多控制报文的可变报头部分包含一个两字节的报文标识符字段。这些报文是 PUBLISH（QoS>0 时），
 * PUBACK，PUBREC，PUBREL，PUBCOMP，SUBSCRIBE, SUBACK，UNSUBSCIBE，
 * UNSUBACK。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public abstract class MqttPacketIdentifierMessage<T extends MqttPacketIdVariableHeader> extends MqttVariableMessage<T> {

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader, T variableHeader) {
        super(mqttFixedHeader);
        setVariableHeader(variableHeader);
    }

}
