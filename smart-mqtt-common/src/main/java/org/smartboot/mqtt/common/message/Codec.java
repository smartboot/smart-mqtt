package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/13
 */
public abstract class Codec {
    protected abstract int preEncode();

    protected abstract void writeTo(MqttWriter mqttWriter) throws IOException;
}
