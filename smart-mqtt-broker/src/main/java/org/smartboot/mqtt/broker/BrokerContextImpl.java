package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.push.PushListener;
import org.smartboot.mqtt.broker.push.impl.PushListenerImpl;
import org.smartboot.mqtt.broker.store.StoredMessage;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class BrokerContextImpl implements BrokerContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerContextImpl.class);
    /**
     * 通过鉴权的连接会话
     */
    private final ConcurrentMap<String, MqttSession> grantSessions = new ConcurrentHashMap<>();
    /**
     *
     */
    private final ConcurrentMap<String, Topic> topicMap = new ConcurrentHashMap<>();
    private final PushListener listener = new PushListenerImpl();
    private final BrokerConfigure brokerConfigure = new BrokerConfigure();
    private final ScheduledExecutorService KEEP_ALIVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    /**
     * Broker Server
     */
    private AioQuickServer server;

    public static StoredMessage asStoredMessage(MqttPublishMessage msg) {
        StoredMessage stored = new StoredMessage(msg.getPayload(), msg.getMqttFixedHeader().getQosLevel(), msg.getMqttPublishVariableHeader().topicName());
        stored.setRetained(msg.getMqttFixedHeader().isRetain());
        return stored;
    }

    @Override
    public void init() throws IOException {
        server = new AioQuickServer(1883, new MqttProtocol(), new MqttBrokerMessageProcessor(this));
        server.start();
        //启动keepalive监听线程
    }

    @Override
    public BrokerConfigure getBrokerConfigure() {
        return brokerConfigure;
    }

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

    @Override
    public void publish(Topic topic, StoredMessage storedMessage) {
        topic.getConsumerGroup().getConsumeOffsets().forEach((mqttSession, consumeOffset) -> {
            LOGGER.info("publish to client:{}", mqttSession.getClientId());
            MqttQoS mqttQoS = storedMessage.getMqttQoS();
            if (mqttQoS.value() > consumeOffset.getMqttQoS().value()) {
                mqttQoS = consumeOffset.getMqttQoS();
            }
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                    .payload(storedMessage.getPayload())
                    .qos(mqttQoS)
                    .packetId(mqttSession.newPacketId()).topicName(topic.getTopic()).build();
            //QoS1 响应监听
            if (publishMessage.getMqttFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
                mqttSession.putInFightMessage(publishMessage.getMqttPublishVariableHeader().packetId(), asStoredMessage(publishMessage));
            }
            mqttSession.write(publishMessage);
        });
    }

    @Override
    public ScheduledExecutorService getKeepAliveThreadPool() {
        return KEEP_ALIVE_EXECUTOR;
    }

    @Override
    public void destroy() {
        server.shutdown();
    }
}
