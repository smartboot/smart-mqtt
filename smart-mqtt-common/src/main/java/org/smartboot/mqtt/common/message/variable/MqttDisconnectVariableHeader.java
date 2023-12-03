/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttDisConnectReturnCode;
import org.smartboot.mqtt.common.message.MqttVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.DisConnectProperties;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttDisconnectVariableHeader extends MqttVariableHeader<DisConnectProperties> {
    /**
     * MQTT5: 断开原因值
     */
    private final MqttDisConnectReturnCode disConnectReturnCode;


    public MqttDisconnectVariableHeader(MqttDisConnectReturnCode disConnectReturnCode, DisConnectProperties properties) {
        super(properties);
        this.disConnectReturnCode = disConnectReturnCode;
    }


    @Override
    protected int preEncode0() {
        return 1;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.writeByte(disConnectReturnCode.getCode());
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
