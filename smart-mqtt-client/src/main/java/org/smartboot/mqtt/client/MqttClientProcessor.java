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
import org.smartboot.mqtt.client.processor.ConnAckProcessor;
import org.smartboot.mqtt.client.processor.MqttAckProcessor;
import org.smartboot.mqtt.client.processor.MqttPingRespProcessor;
import org.smartboot.mqtt.client.processor.MqttProcessor;
import org.smartboot.mqtt.client.processor.PublishProcessor;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientProcessor extends AbstractMessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClientProcessor.class);
    private final MqttClient mqttClient;
    private static final Map<Class<? extends MqttMessage>, MqttProcessor<? extends MqttMessage>> processors = new HashMap<>();

    static {
        processors.put(MqttConnAckMessage.class, new ConnAckProcessor());
        processors.put(MqttPubAckMessage.class, new MqttAckProcessor<MqttPubAckMessage>());
        processors.put(MqttPublishMessage.class, new PublishProcessor());
        processors.put(MqttPubRecMessage.class, new MqttAckProcessor<MqttPubRecMessage>());
        processors.put(MqttPubCompMessage.class, new MqttAckProcessor<MqttPubCompMessage>());
        processors.put(MqttPubRelMessage.class, new MqttAckProcessor<MqttPubRelMessage>());
        processors.put(MqttSubAckMessage.class, new MqttAckProcessor<MqttPubRelMessage>());
        processors.put(MqttPingRespMessage.class, new MqttPingRespProcessor());
    }

    public MqttClientProcessor(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        mqttClient.getEventBus().publish(EventType.RECEIVE_MESSAGE, EventObject.newEventObject(mqttClient, msg));
        MqttProcessor processor = processors.get(msg.getClass());
//        LOGGER.info("receive msg:{}", msg);
        if (processor != null) {
            processor.process(mqttClient, msg);
        } else {
            LOGGER.error("unknown msg:{}", msg);
        }
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//        System.out.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
