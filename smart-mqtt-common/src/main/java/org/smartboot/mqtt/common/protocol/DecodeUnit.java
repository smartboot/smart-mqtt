/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.protocol;

import org.smartboot.mqtt.common.message.MqttMessage;

import java.nio.ByteBuffer;

class DecodeUnit {
    DecoderState state;
    MqttMessage mqttMessage;
    ByteBuffer disposableBuffer;
}
