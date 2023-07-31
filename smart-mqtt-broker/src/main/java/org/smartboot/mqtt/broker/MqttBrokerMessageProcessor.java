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
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.common.DefaultMqttWriter;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.util.MqttAttachKey;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

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

    private final static AttachKey<MqttSession> SESSION_KEY = AttachKey.valueOf(MqttAttachKey.MQTT_SESSION);


    public MqttBrokerMessageProcessor(BrokerContext mqttContext) {
        this.mqttContext = mqttContext;
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        MqttProcessor processor = mqttContext.getMessageProcessors().get(msg.getClass());
        ValidateUtils.notNull(processor, "unSupport message");
        Attachment attachment = session.getAttachment();
        MqttSession mqttSession = attachment.get(SESSION_KEY);
        mqttContext.getEventBus().publish(EventType.RECEIVE_MESSAGE, EventObject.newEventObject(mqttSession, msg));
        mqttSession.setLatestReceiveMessageTime(System.currentTimeMillis());
        processor.process(mqttContext, mqttSession, msg);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case DECODE_EXCEPTION:
                LOGGER.error("decode exception", throwable);
                break;
            case NEW_SESSION: {
                Attachment attachment = new Attachment();
                session.setAttachment(attachment);
                attachment.put(SESSION_KEY, new MqttSession(mqttContext, session, new DefaultMqttWriter(session.writeBuffer())));
                break;
            }
            case SESSION_CLOSED:
                Attachment attachment = session.getAttachment();
                attachment.get(SESSION_KEY).disconnect();
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
