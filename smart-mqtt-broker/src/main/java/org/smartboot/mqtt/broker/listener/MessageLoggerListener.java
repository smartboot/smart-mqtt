package org.smartboot.mqtt.broker.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.listener.MqttSessionListener;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public class MessageLoggerListener implements MqttSessionListener<MqttSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageLoggerListener.class);

    @Override
    public void onMessageReceived(MqttSession session, MqttMessage mqttMessage) {
        if (mqttMessage instanceof MqttPingReqMessage) {
            LOGGER.info("receive ping message from client:{}", session.getClientId());
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("smart-mqtt broker receive messageClass:{} ,data:{}", mqttMessage.getClass().getSimpleName(), mqttMessage);
        }
    }

    @Override
    public void onMessageWrite(MqttSession session, MqttMessage mqttMessage) {
        LOGGER.info("write message:{} to client:{}", mqttMessage.getClass().getSimpleName(), session.getClientId());
    }
}
