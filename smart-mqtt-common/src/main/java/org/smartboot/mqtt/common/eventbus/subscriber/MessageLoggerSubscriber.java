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
