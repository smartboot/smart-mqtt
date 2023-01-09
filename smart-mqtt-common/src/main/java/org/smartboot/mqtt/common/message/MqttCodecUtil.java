package org.smartboot.mqtt.common.message;


import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class MqttCodecUtil {

    public static final int MIN_CLIENT_ID_LENGTH = 1;
    public static final int MAX_CLIENT_ID_LENGTH = 23;
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};

    private MqttCodecUtil() {
    }

    public static MqttFixedHeader resetUnusedFields(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.getMessageType()) {
            case CONNECT:
            case CONNACK:
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case SUBACK:
            case UNSUBACK:
            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                if (mqttFixedHeader.isDup() ||
                        mqttFixedHeader.getQosLevel() != MqttQoS.AT_MOST_ONCE ||
                        mqttFixedHeader.isRetain()) {
                    return new MqttFixedHeader(
                            mqttFixedHeader.getMessageType(),
                            false,
                            MqttQoS.AT_MOST_ONCE,
                            false,
                            mqttFixedHeader.remainingLength());
                }
                return mqttFixedHeader;
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (mqttFixedHeader.isRetain()) {
                    return new MqttFixedHeader(
                            mqttFixedHeader.getMessageType(),
                            mqttFixedHeader.isDup(),
                            mqttFixedHeader.getQosLevel(),
                            false,
                            mqttFixedHeader.remainingLength());
                }
                return mqttFixedHeader;
            default:
                return mqttFixedHeader;
        }
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

    public static String decodeString(ByteBuffer buffer) {
        return decodeString(buffer, 0, Integer.MAX_VALUE);
    }

    /**
     * 每一个字符串都有一个两字节的长度字段作为前缀，它给出这个字符串 UTF-8 编码的字节数，它们在图例
     * 1.1 UTF-8 编码字符串的结构 中描述。因此可以传送的 UTF-8 编码的字符串大小有一个限制，不能超过
     * 65535 字节。
     * 除非另有说明，所有的 UTF-8 编码字符串的长度都必须在 0 到 65535 字节这个范围内。
     */
    public static String decodeString(ByteBuffer buffer, int minBytes, int maxBytes) {
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


    public static int decodeMsbLsb(ByteBuffer buffer) {
        return decodeMsbLsb(buffer, 0, 65535);
    }

    /**
     * 整数数值是 16 位，使用大端序（big-endian，高位字节在低位字节前面）。这意味着一个 16 位的字在网
     * 络上表示为最高有效字节（MSB），后面跟着最低有效字节（LSB）。
     */
    public static int decodeMsbLsb(ByteBuffer buffer, int min, int max) {
        short msbSize = BufferUtils.readUnsignedByte(buffer);
        short lsbSize = BufferUtils.readUnsignedByte(buffer);
        int result = msbSize << 8 | lsbSize;
        if (result < min || result > max) {
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
}
