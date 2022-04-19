package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public abstract class AuthorizedMqttProcessor<T extends MqttMessage> implements MqttProcessor<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizedMqttProcessor.class);

    @Override
    public final void process(BrokerContext context, MqttSession session, T t) {
        if (session.isAuthorized()) {
            process0(context, session, t);
        } else {
            session.disconnect();
            LOGGER.error("clientId:{} is unAuthorized", session.getClientId());
        }
    }

    public abstract void process0(BrokerContext context, MqttSession session, T t);
}
