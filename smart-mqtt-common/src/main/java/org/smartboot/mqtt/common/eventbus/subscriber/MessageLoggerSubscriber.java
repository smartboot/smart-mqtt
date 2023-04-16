/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.eventbus.subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class MessageLoggerSubscriber implements EventBusSubscriber<EventObject<MqttMessage>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageLoggerSubscriber.class);

    @Override
    public void subscribe(EventType<EventObject<MqttMessage>> eventType, EventObject<MqttMessage> object) {
        if (eventType == EventType.RECEIVE_MESSAGE) {
            if (object.getObject() instanceof MqttPingReqMessage) {
                LOGGER.info("receive ping message from client:{}", object.getSession().getClientId());
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("smart-mqtt broker receive messageClass:{} ,data:{}", object.getObject().getClass().getSimpleName(), object.getObject());
            }
        }

        if (eventType == EventType.WRITE_MESSAGE) {
            LOGGER.info("write message:{} to client:{}", object.getObject().getClass().getSimpleName(), object.getSession().getClientId());
        }
    }
}
