/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
class OnlyFixedHeaderMessage extends MqttMessage {
    private static final MqttVariableHeader NONE_VARIABLE_HEADER = new MqttVariableHeader(null) {
        @Override
        public int preEncode0() {
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
