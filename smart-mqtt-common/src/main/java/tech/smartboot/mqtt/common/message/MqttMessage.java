/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message;

import org.smartboot.socket.DecoderException;
import tech.smartboot.mqtt.common.MqttWriter;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.payload.MqttPayload;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public abstract class MqttMessage {
    private static final MqttPayload NONE_PAYLOAD = new MqttPayload() {
        @Override
        protected int preEncode() {
            return 0;
        }

        @Override
        protected void writeTo(MqttWriter mqttWriter) {

        }
    };
    /**
     * 固定报头
     */
    protected final MqttFixedHeader fixedHeader;
    /**
     * 剩余长度
     */
    private int remainingLength;

    public MqttMessage(MqttFixedHeader mqttFixedHeader) {
        this.fixedHeader = mqttFixedHeader;
    }

    public final MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    /**
     * 解码可变头部
     *
     * @param buffer
     */
    public abstract void decodeVariableHeader(ByteBuffer buffer, MqttVersion mqttVersion);

    public void decodePlayLoad(ByteBuffer buffer) {

    }

    public final void write(MqttWriter mqttWriter) throws IOException {
        ValidateUtils.isTrue(mqttWriter.writeSize() == 0, "invalid write size");
        try {
            MqttVariableHeader variableHeader = getVariableHeader();
            MqttPayload mqttPayload = getPayload();
            //剩余长度等于可变报头的长度（10 字节）加上有效载荷的长度。
            int remainingLength = variableHeader.preEncode() + mqttPayload.preEncode();

            //第一部分：固定报头
            fixedHeader.writeTo(mqttWriter);
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, remainingLength);
            int size = mqttWriter.writeSize();
            //第二部分：可变报头
            variableHeader.writeTo(mqttWriter);

            //第三部分：有效载荷
            mqttPayload.writeTo(mqttWriter);
            ValidateUtils.isTrue((mqttWriter.writeSize() - size) == remainingLength, "encode error");
        } finally {
            mqttWriter.reset();
        }
    }

    public abstract MqttVariableHeader getVariableHeader();

    protected MqttPayload getPayload() {
        return NONE_PAYLOAD;
    }


    protected final int decodeMessageId(ByteBuffer buffer) {
        final int messageId = MqttCodecUtil.decodeMsbLsb(buffer);
        if (messageId == 0) {
            throw new DecoderException("invalid messageId: " + messageId);
        }
        return messageId;
    }

    public int getRemainingLength() {
        return remainingLength;
    }

    public void setRemainingLength(int remainingLength) {
        this.remainingLength = remainingLength;
    }
}
