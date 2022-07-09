package org.smartboot.mqtt.common.message;


/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttVariableMessage<T extends MqttVariableHeader> extends MqttMessage {
    /**
     * 可变报头
     */
    private T variableHeader;

    public MqttVariableMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public final T getVariableHeader() {
        return variableHeader;
    }

    protected void setVariableHeader(T variableHeader) {
        this.variableHeader = variableHeader;
    }
}
