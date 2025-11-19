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


import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttVariableMessage<T extends MqttVariableHeader<? extends AbstractProperties>> extends MqttMessage {
    /**
     * 可变报头
     */
    protected T variableHeader;

    /**
     * 可变报头长度
     */
    private int variableHeaderLength;

    MqttVariableMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer, MqttVersion mqttVersion) {
        int position = buffer.position();
        decodeVariableHeader0(buffer, mqttVersion);
        variableHeaderLength = buffer.position() - position;
    }

    protected int getVariableHeaderLength() {
        return variableHeaderLength;
    }

    protected abstract void decodeVariableHeader0(ByteBuffer buffer, MqttVersion mqttVersion);

    public final T getVariableHeader() {
        return variableHeader;
    }

    protected void setVariableHeader(T variableHeader) {
        this.variableHeader = variableHeader;
    }
}
