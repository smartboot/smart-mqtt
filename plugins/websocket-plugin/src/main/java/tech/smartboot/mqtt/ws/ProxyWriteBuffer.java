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

import io.github.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.feat.core.server.WebSocketResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

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
    public void writeLong(long v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] b, int off, int len) {
        outputStream.write(b, off, len);
    }

    @Override
    public void write(ByteBuffer byteBuffer, Runnable releaseCallback) throws IOException {
        throw new UnsupportedEncodingException();
    }


    @Override
    public void flush() {
        if (outputStream.size() == 0) {
            return;
        }
        byte[] bytes = outputStream.toByteArray();
        outputStream.reset();
        response.sendBinaryMessage(bytes);
        response.flush();
    }

    @Override
    public void close() {
        flush();
        response.close();
    }
}
