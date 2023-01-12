package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class OnlyFixedHeaderMessage extends MqttMessage {
    public OnlyFixedHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
    }

    public final void writeWithoutFixedHeader(MqttWriter mqttWriter) {
        mqttWriter.writeByte((byte) 0);
    }
}
