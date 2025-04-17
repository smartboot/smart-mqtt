/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message;

import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

/**
 * 包含报文标识符的消息类型
 * 很多控制报文的可变报头部分包含一个两字节的报文标识符字段。这些报文是 PUBLISH（QoS>0 时），
 * PUBACK，PUBREC，PUBREL，PUBCOMP，SUBSCRIBE, SUBACK，UNSUBSCIBE，
 * UNSUBACK。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public abstract class MqttPacketIdentifierMessage<T extends MqttPacketIdVariableHeader<? extends AbstractProperties>> extends MqttVariableMessage<T> {

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPacketIdentifierMessage(MqttFixedHeader mqttFixedHeader, T variableHeader) {
        super(mqttFixedHeader);
        setVariableHeader(variableHeader);
    }

}
