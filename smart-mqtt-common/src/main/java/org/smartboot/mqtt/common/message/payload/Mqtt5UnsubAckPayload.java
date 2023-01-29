package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/29
 */
public class Mqtt5UnsubAckPayload extends MqttPayload {
    private final byte[] reasonCodes;

    public Mqtt5UnsubAckPayload(byte[] reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    @Override
    protected int preEncode() {
        return reasonCodes.length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.write(reasonCodes);
    }
}
