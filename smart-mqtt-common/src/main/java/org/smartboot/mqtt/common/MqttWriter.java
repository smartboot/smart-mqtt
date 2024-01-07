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

import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
public class MqttWriter {
    private final WriteBuffer writeBuffer;
    private int size;

    public MqttWriter(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    public void reset() {
        size = 0;
    }

    public void writeByte(byte b) {
        size++;
        writeBuffer.writeByte(b);
    }

    public void writeShort(short data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 2;
        writeBuffer.writeShort(data);
    }

    public void writeInt(int data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 4;
        writeBuffer.writeInt(data);
    }

    public synchronized void write(byte[] data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += data.length;
        writeBuffer.write(data);
    }

    public void flush() {
        writeBuffer.flush();
    }

    public int writeSize() {
        return size;
    }
}
