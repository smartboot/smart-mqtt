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

import tech.smartboot.mqtt.common.enums.MqttDisConnectReturnCode;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.variable.MqttDisconnectVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.DisConnectProperties;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttDisconnectMessage extends MqttVariableMessage<MqttDisconnectVariableHeader> {

    public MqttDisconnectMessage(MqttDisconnectVariableHeader mqttConnAckVariableHeader) {
        super(MqttFixedHeader.DISCONNECT_HEADER);
        setVariableHeader(mqttConnAckVariableHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer, final MqttVersion version) {
        if (version == MqttVersion.MQTT_5) {
            byte returnCode = buffer.get();
            DisConnectProperties properties = new DisConnectProperties();
            properties.decode(buffer);
            setVariableHeader(new MqttDisconnectVariableHeader(MqttDisConnectReturnCode.valueOf(returnCode), properties));
        }
    }

    /**
     * MQTT3 的构造方法
     */
    public MqttDisconnectMessage() {
        this(MqttDisconnectVariableHeader.MQTT3_DISCONNECT_VARIABLE_HEADER);
    }
}