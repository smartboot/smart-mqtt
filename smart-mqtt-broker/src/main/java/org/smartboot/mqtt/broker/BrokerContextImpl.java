package org.smartboot.mqtt.broker;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.ConnectIdleTimeMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.KeepAliveMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBusSubscriber;
import org.smartboot.mqtt.broker.eventbus.messagebus.consumer.RetainPersistenceConsumer;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.provider.Providers;
import org.smartboot.mqtt.broker.provider.impl.ConfiguredConnectAuthenticationProviderImpl;
import org.smartboot.mqtt.broker.provider.impl.message.PersistenceMessage;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.transport.AioQuickServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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
    private BrokerConfigure brokerConfigure = new BrokerConfigure();
    /**
     * Keep-Alive监听线程
     */
    private final ScheduledExecutorService KEEP_ALIVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService messageBusExecutorService = Executors.newCachedThreadPool();
    private final MessageBus messageBusSubscriber = new MessageBusSubscriber();
    private final EventBus eventBus = new EventBusImpl(ServerEventType.types());
    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
    private ExecutorService pushThreadPool;
    private ExecutorService retainPushThreadPool;
    private BlockingQueue<BrokerTopic> pushTopicQueue;
    /**
     * Broker Server
     */
    private AioQuickServer server;
    private BufferPagePool pagePool;
    private final MqttBrokerMessageProcessor processor = new MqttBrokerMessageProcessor(this);

    //配置文件内容
    private String configJson;
    private final BrokerTopic SHUTDOWN_TOPIC = new BrokerTopic("");

    @Override
    public void init() throws IOException {

        updateBrokerConfigure();

        initProvider();

        subscribeEventBus();

        subscribeMessageBus();

        loadAndInstallPlugins();

        initPushThread();
        try {
            pagePool = new BufferPagePool(10 * 1024 * 1024, brokerConfigure.getThreadNum(), true);
            server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(), new MqttProtocol(brokerConfigure.getMaxPacketSize()), processor);
            server.setBannerEnabled(false).setReadBufferSize(brokerConfigure.getBufferSize()).setWriteBuffer(brokerConfigure.getBufferSize(), Math.min(brokerConfigure.getMaxInflight(), 16)).setBufferPagePool(pagePool).setThreadNum(Math.max(2, brokerConfigure.getThreadNum()));
            server.start();
            System.out.println(BrokerConfigure.BANNER + "\r\n :: smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");
            System.out.println("❤️Gitee: https://gitee.com/smartboot/smart-mqtt");
            System.out.println("Github: https://github.com/smartboot/smart-mqtt");
            if (StringUtils.isBlank(brokerConfigure.getHost())) {
                System.out.println("\uD83C\uDF89start smart-mqtt success! [port:" + brokerConfigure.getPort() + "]");
            } else {
                System.out.println("\uD83C\uDF89start smart-mqtt success! [host:" + brokerConfigure.getHost() + " port:" + brokerConfigure.getPort() + "]");
            }

        } catch (Exception e) {
            destroy();
            throw e;
        }


        eventBus.publish(ServerEventType.BROKER_STARTED, this);
        //释放内存
        configJson = null;
    }

    private void initProvider() {
        //连接鉴权处理器
        providers.setConnectAuthenticationProvider(new ConfiguredConnectAuthenticationProviderImpl(this));
    }

    private void initPushThread() {
        if (brokerConfigure.getTopicLimit() <= 0) {
            brokerConfigure.setTopicLimit(10);
        }
        pushTopicQueue = brokerConfigure.getTopicLimit() <= 4096 ? new ArrayBlockingQueue<>(brokerConfigure.getTopicLimit()) : new LinkedBlockingQueue<>(brokerConfigure.getTopicLimit());
        retainPushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum());
        pushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum(), new ThreadFactory() {
            int index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-push-" + (index++));
            }
        });

        for (int i = 0; i < getBrokerConfigure().getPushThreadNum(); i++) {
            pushThreadPool.execute(new AsyncTask() {
                @Override
                public void execute() {
                    while (true) {
                        BrokerTopic brokerTopic;
                        try {
                            brokerTopic = pushTopicQueue.take();

                            int size = pushTopicQueue.size();
                            if (size > 1024) {
                                System.out.println("queue:" + size);
                            }
                            //Broker停止服务
                            if (SHUTDOWN_TOPIC == brokerTopic) {
                                pushTopicQueue.put(SHUTDOWN_TOPIC);
                                break;
                            }
                        } catch (InterruptedException e) {
                            LOGGER.error("pushTopicQueue exception", e);
                            break;
                        }
                        try {
                            //存在待输出消息
                            Collection<TopicSubscriber> subscribers = brokerTopic.getConsumeOffsets().values();
                            subscribers.stream().filter(topicSubscriber -> topicSubscriber.isReady() && topicSubscriber.getPushVersion() != brokerTopic.getVersion().get()).forEach(topicSubscriber -> topicSubscriber.batchPublish(BrokerContextImpl.this));
                            brokerTopic.setPushing(false);
                            for (TopicSubscriber subscriber : subscribers) {
                                if (subscriber.getPushVersion() != brokerTopic.getVersion().get()) {
                                    notifyPush(brokerTopic);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("brokerTopic:{} push message exception", brokerTopic.getTopic(), e);
                        }
                    }
                }
            });
        }
    }

    /**
     * 订阅消息总线
     */
    private void subscribeMessageBus() {
        //持久化消息
        messageBusSubscriber.consumer((brokerContext, publishMessage) -> providers.getPersistenceProvider().doSave(publishMessage));
        //消费retain消息
        messageBusSubscriber.consumer(new RetainPersistenceConsumer(), mqttPublishMessage -> mqttPublishMessage.getFixedHeader().isRetain());
    }

    /**
     * 订阅事件总线
     */
    private void subscribeEventBus() {
        eventBus.subscribe(ServerEventType.RECEIVE_PUBLISH_MESSAGE, (eventType, eventObject) -> {
            //进入到消息总线前要先确保BrokerTopic已创建
            BrokerTopic topic = getOrCreateTopic(eventObject.getObject().getVariableHeader().getTopicName());
            try {
                //触发消息总线
                messageBusSubscriber.consume(this, eventObject.getObject());
            } finally {
                eventBus.publish(ServerEventType.MESSAGE_BUS_CONSUMED, topic);
            }
        });
        //连接鉴权超时监控
        eventBus.subscribe(ServerEventType.SESSION_CREATE, new ConnectIdleTimeMonitorSubscriber(this));

        //保持连接状态监听,长时间没有消息通信将断开连接
        eventBus.subscribe(ServerEventType.CONNECT, new KeepAliveMonitorSubscriber(this));

        //消息总线消费完成，触发消息推送
        eventBus.subscribe(ServerEventType.MESSAGE_BUS_CONSUMED, (eventType, brokerTopic) -> {
            brokerTopic.getVersion().incrementAndGet();
            notifyPush(brokerTopic);
        });

        //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
        eventBus.subscribe(ServerEventType.SUBSCRIBE_TOPIC, new EventBusSubscriber<TopicSubscriber>() {
            @Override
            public void subscribe(EventType<TopicSubscriber> eventType, TopicSubscriber subscriber) {
                retainPushThreadPool.execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        AsyncTask task = this;
                        PersistenceMessage storedMessage = providers.getRetainMessageProvider().get(subscriber.getTopic().getTopic(), subscriber.getRetainConsumerOffset());
                        if (storedMessage == null || storedMessage.getCreateTime() > subscriber.getLatestSubscribeTime()) {
                            subscriber.setReady(true);
                            BrokerTopic topic = subscriber.getTopic();
                            notifyPush(topic);

                            //完成retain消息的消费，正式开始监听Topic
                            return;
                        }
                        //retain采用严格顺序publish模式
                        MqttSession session = subscriber.getMqttSession();
                        MqttPublishMessage publishMessage = MqttMessageBuilders.publish().payload(storedMessage.getPayload()).qos(subscriber.getMqttQoS()).packetId(session.newPacketId()).topicName(storedMessage.getTopic()).build();
                        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
                            publishMessage.getVariableHeader().setProperties(new PublishProperties());
                        }
                        InflightQueue inflightQueue = session.getInflightQueue();
                        int index = inflightQueue.offer(publishMessage, storedMessage.getOffset());
                        session.publish(publishMessage, packetId -> {
                            LOGGER.info("publish retain to client:{} success ,message:{} ", session.getClientId(), publishMessage);
                            long offset = inflightQueue.commit(index);
                            if (offset != -1) {
                                subscriber.setRetainConsumerOffset(offset + 1);
                                retainPushThreadPool.execute(task);
                            } else {
                                LOGGER.error("error...");
                            }

                        });
                    }
                });
            }
        });
        eventBus.subscribe(ServerEventType.SUBSCRIBE_REFRESH_TOPIC, (eventType, subscriber) -> {
            LOGGER.info("刷新订阅关系, {} 订阅了topic: {}", subscriber.getTopicFilterToken().getTopicFilter(), subscriber.getTopic().getTopic());
            subscriber.setReady(true);
        });
        //打印消息日志
//        eventBus.subscribe(Arrays.asList(EventType.RECEIVE_MESSAGE, EventType.WRITE_MESSAGE), new
//        MessageLoggerSubscriber());
    }

    private void notifyPush(BrokerTopic topic) {
        if (topic.isPushing()) {
            return;
        }
        synchronized (topic) {
            //已加入推送队列
            if (topic.isPushing()) {
                return;
            }
            try {
                topic.setPushing(true);
                pushTopicQueue.put(topic);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateBrokerConfigure() throws IOException {
        //加载自定义配置文件
        loadYamlConfig();
        brokerConfigure = parseConfig("$.broker", BrokerConfigure.class);

        Properties brokerProperties = new Properties();
        //系统环境变量
        BrokerConfigure.SystemEnvironments.forEach((env, pro) -> {
            String value = System.getenv(env);
            if (value != null) {
                brokerProperties.setProperty(pro, value);
            }
        });
        //系统属性优先级最高
        System.getProperties().stringPropertyNames().forEach(name -> brokerProperties.setProperty(name, System.getProperty(name)));

        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.HOST)) {
            brokerConfigure.setHost(brokerProperties.getProperty(BrokerConfigure.SystemProperty.HOST));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.PORT)) {
            brokerConfigure.setPort(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.PORT)));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.CONNECT_IDLE_TIMEOUT)) {
            brokerConfigure.setNoConnectIdleTimeout(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.CONNECT_IDLE_TIMEOUT)));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.MAX_INFLIGHT)) {
            brokerConfigure.setMaxInflight(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.MAX_INFLIGHT)));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.USERNAME)) {
            brokerConfigure.setUsername(brokerProperties.getProperty(BrokerConfigure.SystemProperty.USERNAME));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.PASSWORD)) {
            brokerConfigure.setPassword(brokerProperties.getProperty(BrokerConfigure.SystemProperty.PASSWORD));
        }
        if (brokerProperties.containsKey(BrokerConfigure.SystemProperty.THREAD_NUM)) {
            brokerConfigure.setThreadNum(Integer.parseInt(brokerProperties.getProperty(BrokerConfigure.SystemProperty.THREAD_NUM)));
        }


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
        plugins.stream().sorted(Comparator.comparingInt(Plugin::order)).forEach(plugin -> {
            LOGGER.info("install plugin: " + plugin.pluginName());
            plugin.install(this);
        });
    }

    @Override
    public BrokerConfigure getBrokerConfigure() {
        return brokerConfigure;
    }

    @Override
    public void addSession(MqttSession session) {
        grantSessions.putIfAbsent(session.getClientId(), session);
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
        return messageBusSubscriber;
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


    @Override
    public <T> T parseConfig(String path, Class<T> clazz) {
        JSONPath jsonPath = JSONPath.of(path);
        JSONReader parser = JSONReader.of(configJson);
        Object result = jsonPath.extract(parser);
        if (result instanceof JSONObject) {
            return ((JSONObject) result).to(clazz);
        } else {
            return null;
        }
    }

    public void loadYamlConfig() throws IOException {
        String brokerConfig = System.getProperty(BrokerConfigure.SystemProperty.BrokerConfig);
        InputStream inputStream = null;

        if (StringUtils.isBlank(brokerConfig)) {
            inputStream = BrokerContext.class.getClassLoader().getResourceAsStream("smart-mqtt.yaml");
            LOGGER.info("load internal yaml config.");
        } else {
            inputStream = Files.newInputStream(Paths.get(brokerConfig));
            LOGGER.info("load external yaml config.");
        }
        Yaml yaml = new Yaml();
        Object object = yaml.load(inputStream);
        configJson = JSONObject.toJSONString(object);
        if (inputStream != null) {
            inputStream.close();
        }
    }

    @Override
    public MqttBrokerMessageProcessor getMessageProcessor() {
        return processor;
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        eventBus.publish(ServerEventType.BROKER_DESTROY, this);
        messageBusExecutorService.shutdown();
        pushTopicQueue.offer(SHUTDOWN_TOPIC);
        pushThreadPool.shutdown();
        server.shutdown();
        pagePool.release();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
