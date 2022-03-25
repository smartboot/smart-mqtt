package org.smartboot.socket.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.push.PushListener;
import org.smartboot.socket.mqtt.push.impl.PushListenerImpl;
import org.smartboot.socket.mqtt.common.Topic;

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
    private final PushListener listener = new PushListenerImpl();

    @Override
    public MqttSession addSession(MqttSession session) {
        return grantSessions.putIfAbsent(session.getClientId(), session);
    }

    public Topic getOrCreateTopic(String topic) {
        return topicMap.computeIfAbsent(topic, Topic::new);
    }

    @Override
    public boolean removeSession(MqttSession session) {
        return grantSessions.remove(session.getClientId(), session);
    }

    @Override
    public MqttSession getSession(String clientId) {
        return grantSessions.get(clientId);
    }

    @Override
    public PushListener getTopicListener() {
        return listener;
    }

}
