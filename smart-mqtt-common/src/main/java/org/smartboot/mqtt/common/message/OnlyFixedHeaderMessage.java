package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.variable.MqttVariableHeader;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class OnlyFixedHeaderMessage extends MqttMessage {
    private static final MqttVariableHeader NONE_VARIABLE_HEADER = new MqttVariableHeader() {
        @Override
        public int preEncode() {
            return 0;
        }

        @Override
        public void writeTo(MqttWriter mqttWriter) throws IOException {

        }
    };

    public OnlyFixedHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
    }

    @Override
    public MqttVariableHeader getVariableHeader() {
        return NONE_VARIABLE_HEADER;
    }
}
