/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.plugin.ws;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.feat.core.server.WebSocketResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
class ByteArrayMqttOutputStream implements MqttWriter {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final WebSocketResponse response;
    private int size;

    public ByteArrayMqttOutputStream(WebSocketResponse response) {
        this.response = response;
    }

    @Override
    public void reset() {
        size = 0;
    }

    @Override
    public void writeByte(byte b) {
        size++;
        outputStream.write(b);
    }

    @Override
    public void writeShort(short data) {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 2;
        outputStream.write((byte) ((data >>> 8) & 0xFF));
        outputStream.write((byte) (data & 0xFF));
    }

    @Override
    public void writeInt(int data) {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 4;
        outputStream.write((byte) ((data >>> 24) & 0xFF));
        outputStream.write((byte) ((data >>> 16) & 0xFF));
        outputStream.write((byte) ((data >>> 8) & 0xFF));
        outputStream.write((byte) (data & 0xFF));
    }

    @Override
    public void write(byte[] data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += data.length;
        outputStream.write(data);
    }

    @Override
    public void flush() {
        byte[] bytes = outputStream.toByteArray();
        outputStream.reset();
        response.sendBinaryMessage(bytes);
        response.flush();
    }

    @Override
    public int writeSize() {
        return size;
    }
}
