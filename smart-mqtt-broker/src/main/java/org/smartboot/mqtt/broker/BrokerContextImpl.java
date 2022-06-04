package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.listener.BrokerLifecycleListener;
import org.smartboot.mqtt.broker.listener.BrokerListeners;
import org.smartboot.mqtt.broker.listener.ConnectIdleTimeListener;
import org.smartboot.mqtt.broker.listener.MessageLoggerListener;
import org.smartboot.mqtt.broker.listener.TopicEventListener;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.provider.Providers;
import org.smartboot.mqtt.broker.store.MessageQueue;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.listener.MqttSessionListener;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.Properties;
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
    private final ConcurrentMap<String, BrokerTopic> topicMap = new ConcurrentHashMap<>();
    private final BrokerConfigure brokerConfigure = new BrokerConfigure();
    /**
     * Keep-Alive监听线程
     */
    private final ScheduledExecutorService KEEP_ALIVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    /**
     * ACK超时监听
     */
    private final ScheduledExecutorService ACK_TIMEOUT_MONITOR_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    /**
     * Push线程池
     */
    private final ExecutorService PUSH_THREAD_POOL = Executors.newFixedThreadPool(brokerConfigure.getPushThreadNum());

    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
    private final BrokerListeners listeners = new BrokerListeners();
    /**
     * Broker Server
     */
    private AioQuickServer server;

    @Override
    public void init() throws IOException {
        updateBrokerConfigure();
        //注册Listener
        addEvent(new ConnectIdleTimeListener(brokerConfigure));
        addEvent(new MessageLoggerListener());

        server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(), new MqttProtocol(), new MqttBrokerMessageProcessor(this));
        server.setBannerEnabled(false);
        server.start();
        System.out.println(BrokerConfigure.BANNER + "\r\n :: smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");
        //启动keepalive监听线程

        loadAndInstallPlugins();
        listeners.getBrokerLifecycleListeners().forEach(listener -> listener.onStarted(this));
    }

    private void updateBrokerConfigure() throws IOException {
        Properties brokerProperties = new Properties();
        //加载默认配置
        System.out.println("aaaaaaaa:  /smart-mqtt.properties");
        System.out.println(this.getClass().getResourceAsStream("/smart-mqtt.properties"));
        brokerProperties.load(this.getClass().getResourceAsStream("/smart-mqtt.properties"));
        //加载自定义配置文件
        String brokerConfig = System.getProperty(BrokerConfigure.SystemProperty.BrokerConfig);
        if (StringUtils.isNotBlank(brokerConfig)) {
            File file = new File(brokerConfig);
            ValidateUtils.isTrue(file.isFile(), "文件不存在");
            FileInputStream fileInputStream = new FileInputStream(file);
            brokerProperties.load(fileInputStream);
        }
        //系统属性优先级最高
        System.getProperties().stringPropertyNames().forEach(name -> brokerProperties.setProperty(name, System.getProperty(name)));

        brokerProperties.stringPropertyNames().forEach(name -> brokerConfigure.setProperty(name, brokerProperties.getProperty(name)));

        brokerConfigure.setHost(brokerProperties.getProperty(BrokerConfigure.SystemProperty.HOST));
        brokerConfigure.setPort(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.PORT, BrokerConfigure.SystemPropertyDefaultValue.PORT)));
        brokerConfigure.setNoConnectIdleTimeout(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.CONNECT_IDLE_TIMEOUT, BrokerConfigure.SystemPropertyDefaultValue.CONNECT_TIMEOUT)));
        brokerConfigure.setMaxInflight(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.MAX_INFLIGHT, BrokerConfigure.SystemPropertyDefaultValue.MAX_INFLIGHT)));
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
            plugin.install(this);
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

    public BrokerTopic getOrCreateTopic(String topic) {
        return topicMap.computeIfAbsent(topic, topicName -> {
            ValidateUtils.isTrue(!MqttUtil.containsTopicWildcards(topicName), "invalid topicName: " + topicName);
            BrokerTopic newTopic = new BrokerTopic(topicName);
            listeners.getTopicEventListeners().forEach(event -> event.onTopicCreate(newTopic));
            return newTopic;
        });
    }

    @Override
    public Collection<BrokerTopic> getTopics() {
        return topicMap.values();
    }

    @Override
    public boolean removeSession(MqttSession session) {
        if (session.getClientId() != null) {
            return grantSessions.remove(session.getClientId(), session);
        } else {
            return false;
        }
    }

    @Override
    public MqttSession getSession(String clientId) {
        return grantSessions.get(clientId);
    }

    @Override
    public void publish(MqttSession session, MqttPublishMessage message) {
        listeners.getTopicEventListeners().forEach(event -> event.onPublish(message));
        StoredMessage storedMessage = providers.getMessageStoreProvider().storeMessage(session.getClientId(), message);
        /**
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (message.getFixedHeader().isRetain()) {
            if (message.getFixedHeader().getQosLevel() == MqttQoS.AT_MOST_ONCE) {
                providers.getRetainMessageProvider().cleanTopic(message.getVariableHeader().getTopicName());
            }
            providers.getRetainMessageProvider().storeRetainMessage(storedMessage);
        }

        PUSH_THREAD_POOL.execute(new AsyncTask() {
            @Override
            public void execute() {
                BrokerTopic topic = getOrCreateTopic(message.getVariableHeader().getTopicName());
                topic.getConsumeOffsets().values().forEach(consumeOffset -> PUSH_THREAD_POOL.execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        batchPublish(consumeOffset);
                    }
                }));
            }
        });
    }

    @Override
    public void publishRetain(TopicSubscriber subscriber) {
        if (!subscriber.getSemaphore().tryAcquire()) {
            LOGGER.error("try acquire fail");
            return;
        }
        //retain采用严格顺序publish模式
        PUSH_THREAD_POOL.execute(new AsyncTask() {
            @Override
            public void execute() {
                AsyncTask task = this;
                StoredMessage storedMessage = providers.getRetainMessageProvider().get(subscriber.getTopic().getTopic(), subscriber.getRetainConsumerOffset(), subscriber.getNextConsumerOffset());
                if (storedMessage == null) {
                    subscriber.getSemaphore().release();
                    batchPublish(subscriber);
                    return;
                }
                MqttSession session = subscriber.getMqttSession();
                MqttPublishMessage publishMessage = MqttUtil.createPublishMessage(session.newPacketId(), storedMessage, subscriber.getMqttQoS());
                InflightQueue inflightQueue = session.getInflightQueue();
                int index = inflightQueue.add(publishMessage, storedMessage.getOffset());
                session.publish(publishMessage, packetId -> {
                    LOGGER.info("publish retain to client:{} success ,message:{} ", session.getClientId(), publishMessage);
                    inflightQueue.commit(index, subscriber::setRetainConsumerOffset);
                    inflightQueue.clear();
                    //本批次全部处理完毕
                    PUSH_THREAD_POOL.execute(task);
                });
            }
        });
    }

    private void batchPublish(TopicSubscriber consumeOffset) {
        if (!consumeOffset.getSemaphore().tryAcquire()) {
            return;
        }
        long nextConsumerOffset = consumeOffset.getNextConsumerOffset();
        MessageQueue messageQueue = providers.getMessageStoreProvider().getStoreQueue(consumeOffset.getTopic().getTopic());
        while (!consumeOffset.getMqttSession().getInflightQueue().isFull()) {
            StoredMessage storedMessage = messageQueue.get(nextConsumerOffset);
            if (storedMessage == null) {
                break;
            }
            nextConsumerOffset++;
            MqttSession mqttSession = consumeOffset.getMqttSession();
            MqttPublishMessage publishMessage = MqttUtil.createPublishMessage(mqttSession.newPacketId(), storedMessage, consumeOffset.getMqttQoS());
            InflightQueue inflightQueue = mqttSession.getInflightQueue();
            int index = inflightQueue.add(publishMessage, storedMessage.getOffset());
            mqttSession.publish(publishMessage, packetId -> {
                //最早发送的消息若收到响应，则更新点位
                boolean done = inflightQueue.commit(index, offset -> consumeOffset.setNextConsumerOffset(offset + 1));
                if (done) {
                    inflightQueue.clear();
                    //本批次全部处理完毕
                    StoredMessage nextMessage = messageQueue.get(consumeOffset.getNextConsumerOffset());
                    if (nextMessage == null) {
                        consumeOffset.getSemaphore().release();
                    } else {
                        PUSH_THREAD_POOL.execute(new AsyncTask() {
                            @Override
                            public void execute() {
                                batchPublish(consumeOffset);
                            }
                        });

                    }
                }
            });
        }
        //无可publish的消息
        if (nextConsumerOffset == consumeOffset.getNextConsumerOffset()) {
            consumeOffset.getSemaphore().release();
        }
        //可能此时正好有新消息投递进来
        if (!consumeOffset.getMqttSession().getInflightQueue().isFull() && messageQueue.get(nextConsumerOffset) != null) {
            batchPublish(consumeOffset);
        }
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
    public BrokerListeners getListeners() {
        return listeners;
    }

    @Override
    public void addEvent(EventListener eventListener) {
        if (eventListener instanceof TopicEventListener) {
            listeners.getTopicEventListeners().add((TopicEventListener) eventListener);
        }
        if (eventListener instanceof BrokerLifecycleListener) {
            listeners.getBrokerLifecycleListeners().add((BrokerLifecycleListener) eventListener);
        }
        if (eventListener instanceof MqttSessionListener) {
            listeners.getSessionListeners().add((MqttSessionListener<MqttSession>) eventListener);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        listeners.getBrokerLifecycleListeners().forEach(listener -> listener.onDestroy(this));
        server.shutdown();
    }
}
