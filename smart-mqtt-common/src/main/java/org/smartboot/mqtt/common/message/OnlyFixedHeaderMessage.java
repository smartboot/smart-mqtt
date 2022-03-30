package org.smartboot.mqtt.common.message;

import org.smartboot.socket.transport.WriteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class OnlyFixedHeaderMessage extends MqttMessage {
    public OnlyFixedHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public final void writeTo(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeBuffer.writeByte((byte) 0);
    }
}
