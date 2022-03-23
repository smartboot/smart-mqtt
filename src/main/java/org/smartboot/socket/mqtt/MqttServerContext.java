package org.smartboot.socket.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttServerContext implements MqttContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttServerContext.class);
    /**
     * 通过鉴权的连接会话
     */
    private final ConcurrentMap<String, MqttSession> grantSessions = new ConcurrentHashMap<>();
    /**
     *
     */
    private final ConcurrentMap<String, Topic> topicMap = new ConcurrentHashMap<>();

    @Override
    public MqttSession addSession(MqttSession session) {
        return grantSessions.putIfAbsent(session.getClientId(), session);
    }

    public Topic getOrCreateTopic(String topic) {
        return topicMap.computeIfAbsent(topic, Topic::new);
    }

    @Override
    public boolean removeSession(MqttSession session) {
        session.getTopicSubscriptions().forEach(topic -> topicMap.get(topic.topicName()).unSubscribe(session.getClientId()));
        return grantSessions.remove(session.getClientId(), session);
    }

    @Override
    public void publish2Subscribers(StoredMessage pubMsg, Topic topic) {
        topic.getSubscribes().forEach(clientId -> {
            MqttSession session = grantSessions.get(clientId);
            System.out.println("分发消息给：" + session);
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                    .payload(ByteBuffer.wrap(pubMsg.getPayload()))
                    .qos(pubMsg.getMqttQoS())
                    .packetId(session.getPacketIdCreator().getAndIncrement())
                    .topicName(topic.getTopic()).build();
            session.write(publishMessage);
        });
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
