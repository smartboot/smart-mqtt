/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
public class DefaultMqttWriter implements MqttWriter {
    private final WriteBuffer writeBuffer;

    public DefaultMqttWriter(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    @Override
    public void writeByte(byte b) {
        writeBuffer.writeByte(b);
    }

    @Override
    public void writeShort(short data) throws IOException {
        writeBuffer.writeShort(data);
    }

    @Override
    public void writeInt(int data) throws IOException {
        writeBuffer.writeInt(data);
    }

    @Override
    public void write(byte[] data) throws IOException {
        writeBuffer.write(data);
    }

    @Override
    public void flush() {
        writeBuffer.flush();
    }
}
