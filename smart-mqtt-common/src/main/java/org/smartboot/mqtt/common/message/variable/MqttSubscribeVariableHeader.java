package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class MqttSubscribeVariableHeader extends MqttPacketIdVariableHeader {

    private SubscribeProperties properties;

    public MqttSubscribeVariableHeader(int packetId) {
        super(packetId);
    }

    public SubscribeProperties getProperties() {
        return properties;
    }

    public void setProperties(SubscribeProperties properties) {
        this.properties = properties;
    }

    @Override
    public int preEncode() {
        // packetId 2 个字节
        int length = 2;
        if (properties != null) {
            length += properties.preEncode();
        }
        return length;
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
