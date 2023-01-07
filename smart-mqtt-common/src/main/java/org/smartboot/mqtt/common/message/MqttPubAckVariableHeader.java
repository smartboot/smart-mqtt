package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.properties.ReasonProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttPubAckVariableHeader extends MqttPacketIdVariableHeader {

    /**
     * 原因码
     */
    private byte reasonCode;
    private final ReasonProperties properties;

    public MqttPubAckVariableHeader(int packetId, ReasonProperties properties) {
        super(packetId);
        this.properties = properties;
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
