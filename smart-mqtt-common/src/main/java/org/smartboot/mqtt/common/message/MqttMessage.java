package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.exception.MqttProcessException;
import org.smartboot.socket.util.DecoderException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.smartboot.mqtt.common.message.MqttCodecUtil.decodeMsbLsb;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttMessage extends ToString {
    /**
     * 8-bit UTF (UCS Transformation Format)
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    protected static final int PACKET_LENGTH = 2;
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};
    private static final int VARIABLE_BYTE_INT_MAX = 268435455;
    private static final int UTF8_STRING_MAX_LENGTH = 65535;
    /**
     * 固定报头
     */
    protected MqttFixedHeader fixedHeader = null;
    protected MqttVersion version;

    public MqttMessage(MqttFixedHeader mqttFixedHeader) {
        this.fixedHeader = mqttFixedHeader;
    }

    public static byte[] encodeMBI(long number) {
        if (number < 0 || number >= VARIABLE_BYTE_INT_MAX) {
            throw new IllegalArgumentException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX);
        }
        int numBytes = 0;
        long no = number;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Encode the remaining length fields in the four bytes
        do {
            byte digit = (byte) (no % 128);
            no = no / 128;
            if (no > 0) {
                digit |= 0x80;
            }
            bos.write(digit);
            numBytes++;
        } while ((no > 0) && (numBytes < 4));

        return bos.toByteArray();
    }


    /**
     * java.io.DataOutputStream#writeUTF(java.lang.String, java.io.DataOutput)
     */
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
            throw new MqttProcessException("encoded string too long: " + utflen + " bytes", () -> {
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

    public final MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    /**
     * 解码可变头部
     *
     * @param buffer
     */
    public void decodeVariableHeader(ByteBuffer buffer) {

    }

    public void decodePlayLoad(ByteBuffer buffer) {

    }

    public void writeTo(MqttWriter mqttWriter) throws IOException {
        throw new UnsupportedOperationException();
    }


    protected final int decodeMessageId(ByteBuffer buffer) {
        final int messageId = decodeMsbLsb(buffer);
        if (messageId == 0) {
            throw new DecoderException("invalid messageId: " + messageId);
        }
        return messageId;
    }

    protected final byte getFixedHeaderByte(MqttFixedHeader header) {
        int ret = 0;
        ret |= header.getMessageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.getQosLevel().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return (byte) ret;
    }

    protected final void writeByteArray(MqttWriter writer, byte[] bytes) throws IOException {
        writer.writeShort((short) bytes.length);
        writer.write(bytes);
    }



    protected final int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num /= 128;
            count++;
        } while (num > 0);
        return count;
    }

    protected final void writeVariableLengthInt(MqttWriter buf, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            buf.writeByte((byte) digit);
        } while (num > 0);
    }

    public MqttVersion getVersion() {
        return version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
    }
}
