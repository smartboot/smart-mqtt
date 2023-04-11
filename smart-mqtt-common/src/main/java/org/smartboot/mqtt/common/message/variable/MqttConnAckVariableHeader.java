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
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.message.MqttVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectAckProperties;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckVariableHeader extends MqttVariableHeader<ConnectAckProperties> {
    /**
     * MQTT3: 连接返回码
     * MQTT5: 连接原因码
     */
    private final MqttConnectReturnCode connectReturnCode;

    /**
     * 当前会话标志使服务端和客户端在是否有已存储的会话状态上保持一致
     */
    private final boolean sessionPresent;


    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent, ConnectAckProperties properties) {
        super(properties);
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
    }

    public MqttConnectReturnCode connectReturnCode() {
        return connectReturnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    @Override
    protected int preEncode0() {
        return 2;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.writeByte((byte) (sessionPresent ? 0x01 : 0x00));
        mqttWriter.writeByte(connectReturnCode.getCode());
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
