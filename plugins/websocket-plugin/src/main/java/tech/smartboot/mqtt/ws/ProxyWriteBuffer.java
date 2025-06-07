/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.ws;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.feat.core.server.WebSocketResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
class ProxyWriteBuffer implements WriteBuffer {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final WebSocketResponse response;

    public ProxyWriteBuffer(WebSocketResponse response) {
        this.response = response;
    }


    @Override
    public void writeByte(byte b) {
        outputStream.write(b);
    }

    @Override
    public void writeShort(short data) {
        outputStream.write((byte) ((data >>> 8) & 0xFF));
        outputStream.write((byte) (data & 0xFF));
    }

    @Override
    public void writeInt(int data) {
        outputStream.write((byte) ((data >>> 24) & 0xFF));
        outputStream.write((byte) ((data >>> 16) & 0xFF));
        outputStream.write((byte) ((data >>> 8) & 0xFF));
        outputStream.write((byte) (data & 0xFF));
    }

    @Override
    public void writeLong(long v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void write(byte[] bytes, int offset, int len, Consumer<WriteBuffer> consumer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void transferFrom(ByteBuffer byteBuffer, Consumer<WriteBuffer> consumer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        byte[] bytes = outputStream.toByteArray();
        outputStream.reset();
        response.sendBinaryMessage(bytes);
        response.flush();
    }

    @Override
    public int chunkCount() {
        return 0;
    }

    @Override
    public void close() {
        flush();
        response.close();
    }
}
