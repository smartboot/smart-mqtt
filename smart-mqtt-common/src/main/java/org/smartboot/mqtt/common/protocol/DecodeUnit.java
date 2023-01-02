package org.smartboot.mqtt.common.protocol;

import org.smartboot.mqtt.common.message.MqttMessage;

import java.nio.ByteBuffer;

class DecodeUnit {
    DecoderState state;
    MqttMessage mqttMessage;
    ByteBuffer disposableBuffer;
}
