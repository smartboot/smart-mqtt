package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.ConnectAuthenticationSubscriber;
import org.smartboot.mqtt.broker.eventbus.ConnectIdleTimeMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.KeepAliveMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.MessageToMessageBusSubscriber;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.messagebus.Message;
import org.smartboot.mqtt.broker.messagebus.MessageBus;
import org.smartboot.mqtt.broker.messagebus.MessageBusImpl;
import org.smartboot.mqtt.broker.messagebus.Subscriber;
import org.smartboot.mqtt.broker.messagebus.subscribe.RetainPersistenceSubscriber;
import org.smartboot.mqtt.broker.persistence.message.PersistenceMessage;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.provider.Providers;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
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
    private ExecutorService pushThreadPool;
    /**
     *
     */
    private final ConcurrentMap<String, BrokerTopic> topicMap = new ConcurrentHashMap<>();
    private final BrokerConfigure brokerConfigure = new BrokerConfigure();
    /**
     * Keep-Alive监听线程
     */
    private final ScheduledExecutorService KEEP_ALIVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final ExecutorService MessageBusExecutorService = Executors.newCachedThreadPool();
    /**
     * ACK超时监听
     */
    private final ScheduledExecutorService ACK_TIMEOUT_MONITOR_EXECUTOR = Executors.newSingleThreadScheduledExecutor();


    private final MessageBus messageBus = new MessageBusImpl(MessageBusExecutorService);

    private final EventBus eventBus = new EventBusImpl(ServerEventType.types());

    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
    /**
     * Broker Server
     */
    private AioQuickServer server;

    @Override
    public void init() throws IOException {
        pushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum());
        updateBrokerConfigure();

        subscribeEventBus();

        subscribeMessageBus();

        server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(), new MqttProtocol(), new MqttBrokerMessageProcessor(this));
        server.setBannerEnabled(false)
                .setReadBufferSize(1024 * 1024)
                .setThreadNum(brokerConfigure.getThreadNum());
        server.start();
        System.out.println(BrokerConfigure.BANNER + "\r\n :: smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");

        loadAndInstallPlugins();
        eventBus.publish(ServerEventType.BROKER_STARTED, this);
    }

    /**
     * 订阅消息总线
     */
    private void subscribeMessageBus() {
        //消费retain消息
        Subscriber retainPersistenceSubscriber = new RetainPersistenceSubscriber(this);
        messageBus.subscribe(retainPersistenceSubscriber, Message::isRetained);
    }

    /**
     * 订阅事件总线
     */
    private void subscribeEventBus() {
        eventBus.subscribe(ServerEventType.RECEIVE_PUBLISH_MESSAGE, new MessageToMessageBusSubscriber(this));
        //连接鉴权超时监控
        eventBus.subscribe(ServerEventType.SESSION_CREATE, new ConnectIdleTimeMonitorSubscriber(this));
        //连接鉴权
        eventBus.subscribe(ServerEventType.CONNECT, new ConnectAuthenticationSubscriber(this));
        //保持连接状态监听,长时间没有消息通信将断开连接
        eventBus.subscribe(ServerEventType.CONNECT, new KeepAliveMonitorSubscriber(this));


        //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
        eventBus.subscribe(ServerEventType.SUBSCRIBE_TOPIC, new EventBusSubscriber<TopicSubscriber>() {
            @Override
            public void subscribe(EventType<TopicSubscriber> eventType, TopicSubscriber subscriber) {
                //retain采用严格顺序publish模式
                pushThreadPool.execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        AsyncTask task = this;
                        PersistenceMessage storedMessage = providers.getRetainMessageProvider().get(subscriber.getTopic().getTopic(), subscriber.getRetainConsumerOffset());
                        if (storedMessage == null || storedMessage.getCreateTime() > subscriber.getLatestSubscribeTime()) {
                            //完成retain消息的消费，正式开始监听Topic

                            subscriber.getMqttSession().batchPublish(subscriber, pushThreadPool);
                            return;
                        }
                        MqttSession session = subscriber.getMqttSession();
                        MqttPublishMessage publishMessage = MqttUtil.createPublishMessage(session.newPacketId(), storedMessage.getTopic(), subscriber.getMqttQoS(), storedMessage.getPayload());
                        InflightQueue inflightQueue = session.getInflightQueue();
                        int index = inflightQueue.add(publishMessage, storedMessage.getOffset());
                        session.publish(publishMessage, packetId -> {
                            LOGGER.info("publish retain to client:{} success ,message:{} ", session.getClientId(), publishMessage);
                            inflightQueue.commit(index, subscriber::setRetainConsumerOffset);
                            inflightQueue.clear();
                            //本批次全部处理完毕
                            pushThreadPool.execute(task);
                        });
                    }
                });
            }
        });
        //打印消息日志
//        eventBus.subscribe(Arrays.asList(EventType.RECEIVE_MESSAGE, EventType.WRITE_MESSAGE), new MessageLoggerSubscriber());
    }

    private void updateBrokerConfigure() throws IOException {
        Properties brokerProperties = new Properties();
        //加载默认配置
        brokerProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("smart-mqtt.properties"));
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
        brokerConfigure.setUsername(brokerProperties.getProperty(BrokerConfigure.SystemProperty.USERNAME));
        brokerConfigure.setPassword(brokerProperties.getProperty(BrokerConfigure.SystemProperty.PASSWORD));
        brokerConfigure.setThreadNum(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.THREAD_NUM, String.valueOf(Runtime.getRuntime().availableProcessors()))));

//        System.out.println("brokerConfigure: " + brokerConfigure);
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

    @Override
    public BrokerTopic getOrCreateTopic(String topic) {
        return topicMap.computeIfAbsent(topic, topicName -> {
            ValidateUtils.isTrue(!MqttUtil.containsTopicWildcards(topicName), "invalid topicName: " + topicName);
            BrokerTopic newTopic = new BrokerTopic(topicName);
            eventBus.publish(ServerEventType.TOPIC_CREATE, newTopic);
            return newTopic;
        });
    }

    @Override
    public Collection<BrokerTopic> getTopics() {
        return topicMap.values();
    }

    @Override
    public MessageBus getMessageBus() {
        return messageBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public MqttSession removeSession(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            LOGGER.warn("clientId is blank, ignore remove grantSession");
            return null;
        }
        return grantSessions.remove(clientId);
    }

    @Override
    public MqttSession getSession(String clientId) {
        return grantSessions.get(clientId);
    }


    @Override
    public ScheduledExecutorService getKeepAliveThreadPool() {
        return KEEP_ALIVE_EXECUTOR;
    }

    @Override
    public Providers getProviders() {
        return providers;
    }

    public void batchPublish(BrokerTopic topic) {

        topic.getConsumeOffsets().values().stream()
                .filter(consumeOffset -> consumeOffset.getSemaphore().availablePermits() > 0)
                .forEach(consumeOffset -> pushThreadPool.execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        consumeOffset.getMqttSession().batchPublish(consumeOffset, pushThreadPool);
                    }
                }));
    }

    @Override
    public ExecutorService pushExecutorService() {
        return pushThreadPool;
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        messageBus.publish(MessageBus.END_MESSAGE);
        eventBus.publish(ServerEventType.BROKER_DESTROY, this);
        MessageBusExecutorService.shutdown();
        server.shutdown();
    }
}
