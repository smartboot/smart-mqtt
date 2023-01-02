package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckVariableHeader extends MqttVariableHeader {
    private final MqttConnectReturnCode connectReturnCode;

    /**
     * 当前会话标志使服务端和客户端在是否有已存储的会话状态上保持一致
     */
    private final boolean sessionPresent;

    private final MqttProperties properties;


    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent) {
        this(connectReturnCode, sessionPresent, null);
    }
    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent, MqttProperties properties) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
        this.properties = properties;
    }

    public MqttConnectReturnCode connectReturnCode() {
        return connectReturnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    public MqttProperties getProperties() {
        return properties;
    }
}
