/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/29
 */
public class Mqtt5UnsubAckPayload extends MqttPayload {
    private final byte[] reasonCodes;

    public Mqtt5UnsubAckPayload(byte[] reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    @Override
    protected int preEncode() {
        return reasonCodes.length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.write(reasonCodes);
    }
}
