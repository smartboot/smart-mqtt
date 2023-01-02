package org.smartboot.mqtt.common.protocol;

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttMessage;

import java.nio.ByteBuffer;

public class DecodeUnit {
    MqttProtocol.DecoderState state;
    public MqttVersion mqttVersion;
    MqttMessage mqttMessage;
    ByteBuffer disposableBuffer;
}
