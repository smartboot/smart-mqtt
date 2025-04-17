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

import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.variable.MqttConnAckVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ConnectAckProperties;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttVariableMessage<MqttConnAckVariableHeader> {


    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(MqttFixedHeader.CONN_ACK_HEADER);
        setVariableHeader(mqttConnAckVariableHeader);
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        final boolean sessionPresent = (buffer.get() & 0x01) == 0x01;
        byte returnCode = buffer.get();

        MqttConnAckVariableHeader variableHeader;
        //MQTT 5.0规范
        if (version == MqttVersion.MQTT_5) {
            ConnectAckProperties properties = new ConnectAckProperties();
            properties.decode(buffer);
            variableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent, properties);
        } else {
            variableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent, null);
        }
        setVariableHeader(variableHeader);
    }
}
