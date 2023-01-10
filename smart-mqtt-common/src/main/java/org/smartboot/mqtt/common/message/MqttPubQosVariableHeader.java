package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.properties.ReasonProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttPubQosVariableHeader extends MqttPacketIdVariableHeader {

    /**
     * 原因码
     */
    private byte reasonCode;
    private ReasonProperties properties;

    public MqttPubQosVariableHeader(int packetId) {
        super(packetId);
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

    public void setProperties(ReasonProperties properties) {
        this.properties = properties;
    }
}
