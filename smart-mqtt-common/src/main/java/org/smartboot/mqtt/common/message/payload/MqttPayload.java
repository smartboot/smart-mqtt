package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.Codec;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/13
 */
public class MqttPayload implements Codec {
    @Override
    public int preEncode() {
        return 0;
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {

    }
}
