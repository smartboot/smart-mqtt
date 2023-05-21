/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.processor.ConnectProcessor;
import org.smartboot.mqtt.broker.processor.DisConnectProcessor;
import org.smartboot.mqtt.broker.processor.MqttAckProcessor;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.processor.PingReqProcessor;
import org.smartboot.mqtt.broker.processor.PubRelProcessor;
import org.smartboot.mqtt.broker.processor.PublishProcessor;
import org.smartboot.mqtt.broker.processor.SubscribeProcessor;
import org.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import org.smartboot.mqtt.common.DefaultMqttWriter;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.Attachment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttBrokerMessageProcessor extends AbstractMessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttBrokerMessageProcessor.class);
    /**
     * Mqtt服务全局Context
     */
    private final BrokerContext mqttContext;
    /**
     * 处于在线状态的会话
     */
    private final Map<String, MqttSession> onlineSessions = new ConcurrentHashMap<>();
    private final Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();

    {
        processorMap.put(MqttPingReqMessage.class, new PingReqProcessor());
        processorMap.put(MqttConnectMessage.class, new ConnectProcessor());
        processorMap.put(MqttPublishMessage.class, new PublishProcessor());
        processorMap.put(MqttSubscribeMessage.class, new SubscribeProcessor());
        processorMap.put(MqttUnsubscribeMessage.class, new UnSubscribeProcessor());
        processorMap.put(MqttPubAckMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttPubRelMessage.class, new PubRelProcessor());
        processorMap.put(MqttPubRecMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttPubCompMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttDisconnectMessage.class, new DisConnectProcessor());
//        addPlugin(new RateLimiterPlugin<>(1024 * 512, 1024 * 512));
    }

    public MqttBrokerMessageProcessor(BrokerContext mqttContext) {
        this.mqttContext = mqttContext;
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        MqttProcessor processor = processorMap.get(msg.getClass());
        if (processor != null) {
            MqttSession mqttSession = onlineSessions.get(session.getSessionID());
            mqttContext.getEventBus().publish(EventType.RECEIVE_MESSAGE, EventObject.newEventObject(mqttSession, msg));
            mqttSession.setLatestReceiveMessageTime(System.currentTimeMillis());
            processor.process(mqttContext, mqttSession, msg);
        } else {
            System.err.println("unSupport message: " + msg);
        }
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case DECODE_EXCEPTION:
                LOGGER.error("decode exception", throwable);
                break;
            case NEW_SESSION:
                session.setAttachment(new Attachment());
                MqttSession mqttSession = new MqttSession(mqttContext, session, new DefaultMqttWriter(session.writeBuffer()));
                onlineSessions.put(session.getSessionID(), mqttSession);
                break;
            case SESSION_CLOSED:
                onlineSessions.remove(session.getSessionID()).disconnect();
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

    public Map<String, MqttSession> getOnlineSessions() {
        return onlineSessions;
    }

}
