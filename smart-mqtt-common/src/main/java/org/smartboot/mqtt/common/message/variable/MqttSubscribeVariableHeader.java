package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class MqttSubscribeVariableHeader extends MqttPacketIdVariableHeader<SubscribeProperties> {


    public MqttSubscribeVariableHeader(int packetId, SubscribeProperties properties) {
        super(packetId, properties);
    }

    @Override
    protected int preEncode0() {
        return 2;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
