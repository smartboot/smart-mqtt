package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class OnlyFixedHeaderMessage extends MqttMessage {
    public OnlyFixedHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public final void writeTo(MqttWriter mqttWriter) {
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));
        mqttWriter.writeByte((byte) 0);
    }
}
