package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.properties.ReasonProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttPubAckVariableHeader extends MqttVariableHeader {
    /**
     * 报文标识符
     */
    private final int packetId;

    /**
     * 原因码
     */
    private byte reasonCode;
    private final ReasonProperties properties;

    public MqttPubAckVariableHeader(int packetId, ReasonProperties properties) {
        this.packetId = packetId;
        this.properties = properties;
    }

    public int getPacketId() {
        return packetId;
    }

    public ReasonProperties getProperties() {
        return properties;
    }

    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }
}
