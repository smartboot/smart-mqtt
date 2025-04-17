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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.common.DefaultMqttWriter;
import tech.smartboot.mqtt.common.MqttMessageProcessor;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
final class MqttBrokerMessageProcessor extends MqttMessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttBrokerMessageProcessor.class);
    /**
     * Mqtt服务全局Context
     */
    private final BrokerContextImpl mqttContext;


    public MqttBrokerMessageProcessor(BrokerContextImpl mqttContext) {
        this.mqttContext = mqttContext;
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        MqttProcessor processor = mqttContext.getMessageProcessors().get(msg.getClass());
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
//        if (throwable != null) {
//            throwable.printStackTrace();
//        }
    }

}
