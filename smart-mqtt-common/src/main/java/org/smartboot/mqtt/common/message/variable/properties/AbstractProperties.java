/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable.properties;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.Codec;
import org.smartboot.mqtt.common.message.MqttCodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/10
 */
public class AbstractProperties extends Codec {
    protected final MqttProperties properties = new MqttProperties();
    private final int validBits;
    private int propertiesLength;

    AbstractProperties(int validBits) {
        this.validBits = validBits;
    }

    public void decode(ByteBuffer buffer) {
        properties.decode(buffer, validBits);
    }

    public final int preEncode() {
        propertiesLength = properties.preEncode(validBits);
        return propertiesLength + MqttCodecUtil.getVariableLengthInt(propertiesLength);
    }

    public final void writeTo(MqttWriter writer) throws IOException {
        //属性长度，编码为变长字节整数。
        MqttCodecUtil.writeVariableLengthInt(writer, propertiesLength);
        properties.writeTo(writer, validBits);
    }
}
