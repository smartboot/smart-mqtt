package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

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

    public MqttPubQosVariableHeader(int packetId, ReasonProperties properties) {
        super(packetId, properties);
    }


    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }

    @Override
    protected int preEncode0() {
        int length = 2;
        if (reasonCode != 0) {
            length += 1;
        }
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        if (reasonCode != 0) {
            mqttWriter.writeByte(reasonCode);
            properties.writeTo(mqttWriter);
        }
    }
}
