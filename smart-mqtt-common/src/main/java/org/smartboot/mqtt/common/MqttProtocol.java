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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttProtocol implements Protocol<MqttMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MqttProtocol.class);
    static final Decoder FINISH_DECODER = (byteBuffer, session) -> null;
    /**
     * websocket负载数据读取成功
     */
    private final MqttHeaderDecoder mqttHeaderDecoder;

    public MqttProtocol(int maxBytesInMessage) {
        mqttHeaderDecoder = new MqttHeaderDecoder(maxBytesInMessage);
    }


    @Override
    public MqttMessage decode(ByteBuffer buffer, AioSession session) {
        AbstractSession mqttSession = session.getAttachment();
        Decoder decodeChain = mqttSession.decoder;
        if (decodeChain == null) {
            decodeChain = mqttHeaderDecoder;
        }
        decodeChain = decodeChain.decode(buffer, mqttSession);
        if (decodeChain == FINISH_DECODER) {
            MqttMessage mqttMessage = mqttSession.mqttMessage;
            mqttSession.decoder = mqttHeaderDecoder;
            mqttSession.mqttMessage = null;
            return mqttMessage;
        } else {
            mqttSession.decoder = decodeChain;
            return null;
        }
    }
}
