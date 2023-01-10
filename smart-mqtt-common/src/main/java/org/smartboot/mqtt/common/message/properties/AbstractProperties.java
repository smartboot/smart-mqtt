package org.smartboot.mqtt.common.message.properties;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/10
 */
class AbstractProperties {
    protected final MqttProperties properties = new MqttProperties();
    private final int validBits;

    public AbstractProperties(int validBits) {
        this.validBits = validBits;
    }

    public void decode(ByteBuffer buffer) {
        properties.decode(buffer, validBits);
    }

    public final int preEncode() {
        return properties.preEncode(validBits);
    }

    public final void writeTo(MqttWriter writer) throws IOException {
        properties.writeTo(writer, validBits);
    }
}
