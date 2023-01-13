package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;

import java.io.IOException;

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

    @Override
    public int preEncode() {
        int length = 2;
        if (reasonCode != 0) {
            length += 1 + properties.preEncode();
        }
        return length;
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        if (reasonCode != 0) {
            mqttWriter.writeByte(reasonCode);
            properties.writeTo(mqttWriter);
        }
    }
}
