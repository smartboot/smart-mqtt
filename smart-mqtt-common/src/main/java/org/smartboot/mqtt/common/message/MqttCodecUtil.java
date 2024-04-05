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
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.util.TopicByteTree;
import org.smartboot.socket.DecoderException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class MqttCodecUtil {

    private MqttCodecUtil() {
    }

    /**
     * 解码变长字节整数，规范1.5.5
     */
    public static int decodeVariableByteInteger(ByteBuffer buffer) {
        int multiplier = 1;
        int value = 0;
        byte encodedByte;
        do {
            encodedByte = buffer.get();
            value += (encodedByte & 127) * multiplier;
            if (multiplier > 128 * 128 * 128) throw new DecoderException("decode Variable Byte Integer error");
            multiplier *= 128;
        } while ((encodedByte & 128) != 0);
        return value;
    }

    public static void writeVariableLengthInt(MqttWriter buf, int num) throws IOException {
        if (num <= 127) {
            buf.writeByte((byte) num);
        } else if (num <= 16383) {
            buf.writeShort((short) (((num | 0x80) << 8) | (num >>> 7)));
        } else if (num <= 268435455) {
            do {
                int digit = num & 0x7F; // 取低7位
                num >>>= 7; // 无符号右移7位
                if (num != 0) {
                    digit |= 0x80; // 如果还有更多的字节，设置最高位为1
                }
                buf.writeByte((byte) digit);
            } while (num != 0);
        } else {
            throw new IOException("payload too large");
        }
    }

    public static String decodeUTF8(ByteBuffer buffer) {
        return decodeUTF8(buffer, 0, Integer.MAX_VALUE);
    }

    public static final TopicByteTree cache = new TopicByteTree();

    public static TopicByteTree scanTopicTree(ByteBuffer buffer, TopicByteTree cache) {
        final int size = decodeMsbLsb(buffer);
        return cache.search(buffer, size, true);
    }


    /**
     * 每一个字符串都有一个两字节的长度字段作为前缀，它给出这个字符串 UTF-8 编码的字节数，它们在图例
     * 1.1 UTF-8 编码字符串的结构 中描述。因此可以传送的 UTF-8 编码的字符串大小有一个限制，不能超过
     * 65535 字节。
     * 除非另有说明，所有的 UTF-8 编码字符串的长度都必须在 0 到 65535 字节这个范围内。
     */
    public static String decodeUTF8(ByteBuffer buffer, int minBytes, int maxBytes) {
        final int size = decodeMsbLsb(buffer);
        if (size < minBytes || size > maxBytes) {
            throw new DecoderException("invalid string length " + size);
        }
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] encodeUTF8(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > 65535) {
            throw new MqttException("encoded string too long: " + bytes.length + " bytes", () -> {
            });
        }
        byte[] bytearr = new byte[bytes.length + 2];

        bytearr[0] = (byte) ((bytes.length >>> 8) & 0xFF);
        bytearr[1] = (byte) (bytes.length & 0xFF);
        System.arraycopy(bytes, 0, bytearr, 2, bytes.length);
        return bytearr;
    }

    /**
     * 整数数值是 16 位，使用大端序（big-endian，高位字节在低位字节前面）。这意味着一个 16 位的字在网
     * 络上表示为最高有效字节（MSB），后面跟着最低有效字节（LSB）。
     */
    public static int decodeMsbLsb(ByteBuffer buffer) {
        return buffer.getShort() & 0xffff;
    }

    public static void writeMsbLsb(MqttWriter writer, int v) throws IOException {
        writer.writeShort((short) v);
    }

    public static byte[] decodeByteArray(ByteBuffer buffer) {
        final int decodedSize = decodeMsbLsb(buffer);
        byte[] bytes = new byte[decodedSize];
        buffer.get(bytes);
        return bytes;
    }


    public static int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num >>>= 7;
            count++;
        } while (num > 0);
        return count;
    }

}
