package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.plugins.Plugin;
import org.smartboot.mqtt.broker.provider.Providers;
import org.smartboot.mqtt.broker.store.StoredMessage;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
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
    private final BrokerConfigure brokerConfigure = new BrokerConfigure();
    /**
     * Keep-Alive监听线程
     */
    private final ScheduledExecutorService KEEP_ALIVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    /**
     * Push线程池
     */
    private final ExecutorService PUSH_THREAD_POOL = Executors.newFixedThreadPool(brokerConfigure.getPushThreadNum());

    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
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

        loadAndInstallPlugins();
    }

    /**
     * 加载并安装插件
     */
    private void loadAndInstallPlugins() {
        for (Plugin plugin : ServiceLoader.load(Plugin.class, Providers.class.getClassLoader())) {
            LOGGER.info("load plugin: " + plugin.pluginName());
            plugins.add(plugin);
        }
        //安装插件
        plugins.forEach(plugin -> {
            LOGGER.info("install plugin: " + plugin.pluginName());
            plugin.install(providers);
        });
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
        return topicMap.computeIfAbsent(topic, topicName -> {
            ValidateUtils.isTrue(!MqttUtil.containsTopicWildcards(topicName), "invalid topicName: " + topicName);
            Topic newTopic = new Topic(topicName);
            //采用通配符且未匹配上的订阅者尝试重新匹配
            providers.getTopicFilterProvider().rematch(newTopic);
            return newTopic;
        });
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
    public void publish(Topic topic, MqttQoS mqttQoS, byte[] payload) {
        PUSH_THREAD_POOL.execute(() -> topic.getConsumerGroup().getConsumeOffsets().forEach((mqttSession, consumeOffset) -> {
            LOGGER.info("publish to client:{}", mqttSession.getClientId());
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish().payload(payload).qos(mqttQoS.value() > consumeOffset.getMqttQoS().value() ? consumeOffset.getMqttQoS() : mqttQoS).packetId(mqttSession.newPacketId()).topicName(topic.getTopic()).build();
            //QoS1 响应监听
            if (publishMessage.getMqttFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
                mqttSession.putInFightMessage(publishMessage.getMqttPublishVariableHeader().packetId(), asStoredMessage(publishMessage));
            }
            mqttSession.write(publishMessage);
        }));
    }

    @Override
    public ScheduledExecutorService getKeepAliveThreadPool() {
        return KEEP_ALIVE_EXECUTOR;
    }

    @Override
    public Providers getProviders() {
        return providers;
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        server.shutdown();
    }
}
