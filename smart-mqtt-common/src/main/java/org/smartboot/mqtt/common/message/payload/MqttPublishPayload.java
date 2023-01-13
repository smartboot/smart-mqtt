package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/13
 */
public class MqttPublishPayload extends MqttPayload {
    private final byte[] payload;

    public MqttPublishPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public int preEncode() {
        return payload.length;
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.write(payload);
    }

    public byte[] getPayload() {
        return payload;
    }
}
