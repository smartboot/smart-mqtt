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
import org.smartboot.socket.DecoderException;
import org.smartboot.socket.util.BufferUtils;

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

    public static void writeVariableLengthInt(MqttWriter buf, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            buf.writeByte((byte) digit);
        } while (num > 0);
    }

    public static String decodeUTF8(ByteBuffer buffer) {
        return decodeUTF8(buffer, 0, Integer.MAX_VALUE);
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
//            buffer.position(buffer.position() + size);
//            return null;
            throw new DecoderException("invalid string length " + size);
        }
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] encodeUTF8(String str) {
        int strlen = str.length();
        int utflen = 0;
        int c, count = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535) {
            throw new MqttException("encoded string too long: " + utflen + " bytes", () -> {
            });
        }
        byte[] bytearr = new byte[utflen + 2];

        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int i = 0;
        for (i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) break;
            bytearr[count++] = (byte) c;
        }

        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;

            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            }
        }
        return bytearr;
    }

    /**
     * 整数数值是 16 位，使用大端序（big-endian，高位字节在低位字节前面）。这意味着一个 16 位的字在网
     * 络上表示为最高有效字节（MSB），后面跟着最低有效字节（LSB）。
     */
    public static int decodeMsbLsb(ByteBuffer buffer) {
        short msbSize = BufferUtils.readUnsignedByte(buffer);
        short lsbSize = BufferUtils.readUnsignedByte(buffer);
        int result = msbSize << 8 | lsbSize;
        if (result < 0 || result > 65535) {
            result = -1;
        }
        return result;
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

    public static void writeByteArray(MqttWriter writer, byte[] bytes) throws IOException {
        writer.writeShort((short) bytes.length);
        writer.write(bytes);
    }


    public static int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num /= 128;
            count++;
        } while (num > 0);
        return count;
    }

    public static void writeFixedHeader(MqttWriter writer, MqttFixedHeader header) {
        int ret = 0;
        ret |= header.getMessageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.getQosLevel().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        writer.writeByte((byte) ret);
    }
}
