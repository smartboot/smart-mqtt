/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.client;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.client.processor.MqttAckProcessor;
import tech.smartboot.mqtt.client.processor.MqttProcessor;
import tech.smartboot.mqtt.client.processor.PubRelProcessor;
import tech.smartboot.mqtt.client.processor.PublishProcessor;
import tech.smartboot.mqtt.common.MqttMessageProcessor;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPingRespMessage;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubCompMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubAckMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientProcessor extends MqttMessageProcessor {
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
        MqttClient client = session.getAttachment();
        MqttProcessor processor = processors.get(msg.getClass());
        if (processor != null) {
            processor.process(client, msg);
        } else {
            throw new IllegalStateException("unsupported message type: " + msg.getClass().getSimpleName());
        }
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case DECODE_EXCEPTION:
                System.err.println("decodeException");
                throwable.printStackTrace();
                break;
            case SESSION_CLOSED:
                MqttClient client = session.getAttachment();
                client.release();
                break;
            case PROCESS_EXCEPTION:
                if (throwable instanceof MqttException) {
                    System.err.println("processException");
                    throwable.printStackTrace();
                    ((MqttException) throwable).getCallback().run();
                }
                break;
            default:
                break;
        }
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
