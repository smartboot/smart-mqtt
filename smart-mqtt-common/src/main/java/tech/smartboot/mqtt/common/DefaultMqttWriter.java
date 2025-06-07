/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
public class DefaultMqttWriter implements MqttWriter {
    private final WriteBuffer writeBuffer;
    private int size;

    public DefaultMqttWriter(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    @Override
    public void reset() {
        size = 0;
    }

    @Override
    public void writeByte(byte b) {
        size++;
        writeBuffer.writeByte(b);
    }

    @Override
    public void writeShort(short data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 2;
        writeBuffer.writeShort(data);
    }

    @Override
    public void writeInt(int data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += 4;
        writeBuffer.writeInt(data);
    }

    @Override
    public void write(byte[] data) throws IOException {
        ValidateUtils.isTrue(size != 0, "error: writeShort can't write data, because writer is empty");
        size += data.length;
        writeBuffer.write(data);
    }

    @Override
    public void flush() {
        writeBuffer.flush();
    }

    @Override
    public int writeSize() {
        return size;
    }
}
