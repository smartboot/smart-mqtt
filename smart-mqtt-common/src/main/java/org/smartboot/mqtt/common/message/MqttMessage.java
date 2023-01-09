package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.socket.util.DecoderException;

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


    /**
     * java.io.DataOutputStream#writeUTF(java.lang.String, java.io.DataOutput)
     */


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




    public MqttVersion getVersion() {
        return version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
    }
}
