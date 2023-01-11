package org.smartboot.mqtt.common.message.variable;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttPubQosVariableHeader extends MqttReasonVariableHeader {

    /**
     * 原因码
     */
    private byte reasonCode;

    public MqttPubQosVariableHeader(int packetId) {
        super(packetId);
    }


    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }

}
