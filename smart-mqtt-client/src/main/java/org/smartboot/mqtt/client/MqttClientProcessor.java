/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.processor.MqttAckProcessor;
import org.smartboot.mqtt.client.processor.MqttProcessor;
import org.smartboot.mqtt.client.processor.PubRelProcessor;
import org.smartboot.mqtt.client.processor.PublishProcessor;
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.util.MqttAttachKey;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientProcessor extends AbstractMessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClientProcessor.class);
    final static AttachKey<MqttClient> SESSION_KEY = AttachKey.valueOf(MqttAttachKey.MQTT_SESSION);
    private static final Map<Class<? extends MqttMessage>, MqttProcessor<? extends MqttMessage>> processors = new HashMap<>();

    static {
        processors.put(MqttConnAckMessage.class, (MqttProcessor<MqttConnAckMessage>) MqttClient::receiveConnAckMessage);
        processors.put(MqttPubAckMessage.class, new MqttAckProcessor<MqttPubAckMessage>());
        processors.put(MqttPublishMessage.class, new PublishProcessor());
        processors.put(MqttPubRecMessage.class, new MqttAckProcessor<MqttPubRecMessage>());
        processors.put(MqttPubCompMessage.class, new MqttAckProcessor<MqttPubCompMessage>());
        processors.put(MqttPubRelMessage.class, new PubRelProcessor());
        processors.put(MqttSubAckMessage.class, new MqttAckProcessor<MqttSubAckMessage>());
        processors.put(MqttPingRespMessage.class, (MqttProcessor<MqttPingRespMessage>) (mqttClient, message) -> mqttClient.pingTimeout = 0);
    }


    @Override
    public void process0(AioSession session, MqttMessage msg) {
        Attachment attachment = session.getAttachment();
        MqttClient client = attachment.get(SESSION_KEY);
        MqttProcessor processor = processors.get(msg.getClass());
        if (processor != null) {
            processor.process(client, msg);
        } else {
            LOGGER.error("unknown msg:{}", msg);
        }
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case DECODE_EXCEPTION:
                LOGGER.error("decode exception", throwable);
                break;
            case SESSION_CLOSED:
                Attachment attachment = session.getAttachment();
                attachment.get(SESSION_KEY).release();
                break;
            case PROCESS_EXCEPTION:
                if (throwable instanceof MqttException) {
                    LOGGER.warn("process exception", throwable);
                    ((MqttException) throwable).getCallback().run();
                }
                break;
            default:
                break;
        }
//        if (throwable != null) {
//            throwable.printStackTrace();
//        }
    }
}
