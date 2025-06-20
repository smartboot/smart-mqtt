/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.broker.processor.ConnectProcessor;
import tech.smartboot.mqtt.broker.processor.DisConnectProcessor;
import tech.smartboot.mqtt.broker.processor.MqttAckProcessor;
import tech.smartboot.mqtt.broker.processor.PingReqProcessor;
import tech.smartboot.mqtt.broker.processor.PubRelProcessor;
import tech.smartboot.mqtt.broker.processor.PublishProcessor;
import tech.smartboot.mqtt.broker.processor.SubscribeProcessor;
import tech.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import tech.smartboot.mqtt.common.DefaultMqttWriter;
import tech.smartboot.mqtt.common.MqttMessageProcessor;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttDisconnectMessage;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPingReqMessage;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubCompMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
final class MqttBrokerMessageProcessor extends MqttMessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttBrokerMessageProcessor.class);
    /**
     * MQTT消息处理器映射表。
     * <p>
     * Key为MQTT消息类型，Value为对应的消息处理器。
     * 支持所有MQTT协议定义的消息类型，包括：
     * <ul>
     *   <li>连接消息（CONNECT）</li>
     *   <li>发布消息（PUBLISH）</li>
     *   <li>订阅消息（SUBSCRIBE）</li>
     *   <li>确认消息（PUBACK等）</li>
     * </ul>
     * </p>
     */
    private final Map<Class<? extends MqttMessage>, MqttProcessor<?, ?, ?>> processors;
    /**
     * Mqtt服务全局Context
     */
    private final BrokerContextImpl mqttContext;


    {
        Map<Class<? extends MqttMessage>, MqttProcessor<?, ?, ?>> mqttProcessors = new HashMap<>();
        mqttProcessors.put(MqttPingReqMessage.class, new PingReqProcessor());
        mqttProcessors.put(MqttConnectMessage.class, new ConnectProcessor());
        mqttProcessors.put(MqttPublishMessage.class, new PublishProcessor());
        mqttProcessors.put(MqttSubscribeMessage.class, new SubscribeProcessor());
        mqttProcessors.put(MqttUnsubscribeMessage.class, new UnSubscribeProcessor());
        mqttProcessors.put(MqttPubAckMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttPubRelMessage.class, new PubRelProcessor());
        mqttProcessors.put(MqttPubRecMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttPubCompMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttDisconnectMessage.class, new DisConnectProcessor());
        processors = Collections.unmodifiableMap(mqttProcessors);
    }

    public MqttBrokerMessageProcessor(BrokerContextImpl mqttContext) {
        this.mqttContext = mqttContext;
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        MqttProcessor processor = processors.get(msg.getClass());
        ValidateUtils.notNull(processor, "unSupport message");
        MqttSessionImpl mqttSession = session.getAttachment();
        if (!EventBusImpl.RECEIVE_MESSAGE_SUBSCRIBER_LIST.isEmpty()) {
            mqttContext.getEventBus().publish(EventType.RECEIVE_MESSAGE, EventObject.newEventObject(mqttSession, msg), EventBusImpl.RECEIVE_MESSAGE_SUBSCRIBER_LIST);
        }
        mqttSession.setLatestReceiveMessageTime(MqttUtil.currentTimeMillis());
        processor.process(mqttContext, mqttSession, msg);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case DECODE_EXCEPTION:
                LOGGER.error("decode exception", throwable);
                break;
            case NEW_SESSION: {
                session.setAttachment(new MqttSessionImpl(mqttContext, session, new DefaultMqttWriter(session.writeBuffer())));
                break;
            }
            case SESSION_CLOSED:
                MqttSessionImpl mqttSession = session.getAttachment();
                mqttSession.disconnect();
                break;
            case PROCESS_EXCEPTION:
                if (throwable instanceof MqttException) {
                    LOGGER.warn("process exception", throwable);
                    ((MqttException) throwable).getCallback().run();
                } else {
                    throwable.printStackTrace();
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
