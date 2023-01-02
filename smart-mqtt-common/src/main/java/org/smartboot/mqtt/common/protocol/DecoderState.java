package org.smartboot.mqtt.common.protocol;

enum DecoderState {
    READ_FIXED_HEADER, READ_VARIABLE_HEADER, READ_PAYLOAD, FINISH,
}