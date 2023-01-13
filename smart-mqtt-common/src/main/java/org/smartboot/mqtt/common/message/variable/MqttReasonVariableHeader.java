package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class MqttReasonVariableHeader extends MqttPacketIdVariableHeader {

    protected ReasonProperties properties;

    public MqttReasonVariableHeader(int packetId) {
        super(packetId);
    }

    public final ReasonProperties getProperties() {
        return properties;
    }

    public final void setProperties(ReasonProperties properties) {
        this.properties = properties;
    }

    @Override
    protected int preEncode() {
        // packetId 2 个字节
        int length = 2;
        if (properties != null) {
            length += properties.preEncode();
        }
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
