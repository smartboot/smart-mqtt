package org.smartboot.mqtt.common.message;


import org.smartboot.mqtt.common.message.variable.MqttVariableHeader;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttVariableMessage<T extends MqttVariableHeader> extends MqttMessage {
    /**
     * 可变报头
     */
    protected T variableHeader;

    /**
     * 可变报头长度
     */
    private int variableHeaderLength;

    public MqttVariableMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
        int position = buffer.position();
        decodeVariableHeader0(buffer);
        variableHeaderLength = buffer.position() - position;
    }

    protected int getVariableHeaderLength() {
        return variableHeaderLength;
    }

    protected abstract void decodeVariableHeader0(ByteBuffer buffer);

    public final T getVariableHeader() {
        return variableHeader;
    }

    protected void setVariableHeader(T variableHeader) {
        this.variableHeader = variableHeader;
    }
}
