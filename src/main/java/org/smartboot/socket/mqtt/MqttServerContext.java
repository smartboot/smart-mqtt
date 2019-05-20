package org.smartboot.socket.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttServerContext implements MqttContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttServerContext.class);
    private final ConcurrentMap<String, MqttSession> mqttSessionMap = new ConcurrentHashMap<>();

    @Override
    public MqttSession addSession(MqttSession session) {
        return mqttSessionMap.putIfAbsent(session.getClientId(), session);
    }

    @Override
    public boolean removeSession(MqttSession session) {
        return mqttSessionMap.remove(session.getClientId(), session);
    }

    @Override
    public void publish2Subscribers(StoredMessage pubMsg, Topic topic) {

    }

    @Override
    public void publish2Subscribers(StoredMessage pubMsg, Topic topic, int messageID) {
        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Sending publish message to subscribers. ClientId={}, topic={}, messageId={}, payload={}, " +
//                            "subscriptionTree={}", pubMsg.getClientID(), topic, messageID, DebugUtils.payload2Str(pubMsg.getPayload()),
//                    subscriptions.dumpTree());
            LOGGER.info("Sending publish message to subscribers. ClientId={}, topic={}, messageId={}", pubMsg.getClientID(), topic,
                    messageID);
        } else {
            LOGGER.info("Sending publish message to subscribers. ClientId={}, topic={}, messageId={}", pubMsg.getClientID(), topic,
                    messageID);
        }
        publish2Subscribers(pubMsg, topic);
    }
}
