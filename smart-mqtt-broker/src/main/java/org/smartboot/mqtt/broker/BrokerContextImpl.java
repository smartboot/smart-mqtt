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
import org.smartboot.mqtt.common.enums.MqttMetricEnum;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.to.MetricItemTO;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.extension.plugins.AbstractPlugin;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final BrokerRuntime runtime = new BrokerRuntime();
    private ExecutorService pushThreadPool;
    private ExecutorService retainPushThreadPool;
    private ExecutorService executorService;
    private BlockingQueue<BrokerTopic> pushTopicQueue;
    private ConcurrentLinkedQueue<Runnable> pushTaskQueue;
    /**
     * Broker Server
     */
    private AioQuickServer server;
    private BufferPagePool pagePool;
    private final MqttBrokerMessageProcessor processor = new MqttBrokerMessageProcessor(this);

    //配置文件内容
    private String configJson;
    private final BrokerTopic SHUTDOWN_TOPIC = new BrokerTopic("");

    /**
     * 统计指标
     */
    private final Map<MqttMetricEnum, MetricItemTO> metricMap = new HashMap<>();
    private AsynchronousChannelGroup asynchronousChannelGroup;

    @Override
    public void init() throws IOException {

        updateBrokerConfigure();

        initProvider();

        subscribeEventBus();

        subscribeMessageBus();

        initPushThread();

        initMetric();

        loadAndInstallPlugins();


        try {
            asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                int i;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "smart-mqtt-broker-" + (++i));
                }
            });
            pagePool = new BufferPagePool(10 * 1024 * 1024, brokerConfigure.getThreadNum(), true);
            server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(), new MqttProtocol(brokerConfigure.getMaxPacketSize()), processor);
            server.setBannerEnabled(false).setReadBufferSize(brokerConfigure.getBufferSize()).setWriteBuffer(brokerConfigure.getBufferSize(), Math.min(brokerConfigure.getMaxInflight(), 16)).setBufferPagePool(pagePool).setThreadNum(Math.max(2, brokerConfigure.getThreadNum()));
            server.start(asynchronousChannelGroup);
            System.out.println(BrokerConfigure.BANNER + "\r\n :: smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");
            System.out.println("❤️Gitee: https://gitee.com/smartboot/smart-mqtt");
            System.out.println("Github: https://github.com/smartboot/smart-mqtt");
            if (StringUtils.isBlank(brokerConfigure.getHost())) {
                System.out.println("\uD83C\uDF89start smart-mqtt success! [port:" + brokerConfigure.getPort() + "]");
            } else {
                System.out.println("\uD83C\uDF89start smart-mqtt success! [host:" + brokerConfigure.getHost() + " port:" + brokerConfigure.getPort() + "]");
            }
            if (StringUtils.isBlank(brokerConfigure.getName())) {
                runtime.setName("smart-mqtt@" + (StringUtils.isBlank(brokerConfigure.getHost()) ? "0.0.0.0" : brokerConfigure.getHost()));
            } else {
                runtime.setName(brokerConfigure.getName());
            }

            runtime.setStartTime(System.currentTimeMillis());
            runtime.setPid(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
            runtime.setIpAddress(brokerConfigure.getHost() + ":" + brokerConfigure.getPort());
        } catch (Exception e) {
            destroy();
            throw e;
        }


        eventBus.publish(ServerEventType.BROKER_STARTED, this);
        //释放内存
        configJson = null;
    }

    private void initMetric() {
        for (MqttMetricEnum metricEnum : MqttMetricEnum.values()) {
            metricMap.put(metricEnum, new MetricItemTO(metricEnum));
        }

        processor.addPlugin(new AbstractPlugin<MqttMessage>() {
            @Override
            public void afterRead(AioSession session, int readSize) {
                if (readSize > 0) {
                    metricMap.get(MqttMetricEnum.BYTES_RECEIVED).getMetric().add(readSize);
                }
            }

            @Override
            public void afterWrite(AioSession session, int writeSize) {
                if (writeSize > 0) {
                    metricMap.get(MqttMetricEnum.BYTES_SENT).getMetric().add(writeSize);
                }
            }
        });
        eventBus.subscribe(ServerEventType.CONNECT, (eventType, object) -> metricMap.get(MqttMetricEnum.CLIENT_CONNECT).getMetric().increment());
        eventBus.subscribe(ServerEventType.DISCONNECT, (eventType, object) -> metricMap.get(MqttMetricEnum.CLIENT_DISCONNECT).getMetric().increment());
        eventBus.subscribe(ServerEventType.SUBSCRIBE_ACCEPT, (eventType, object) -> metricMap.get(MqttMetricEnum.CLIENT_SUBSCRIBE).getMetric().increment());
        eventBus.subscribe(ServerEventType.UNSUBSCRIBE_ACCEPT, (eventType, object) -> metricMap.get(MqttMetricEnum.CLIENT_UNSUBSCRIBE).getMetric().increment());
        eventBus.subscribe(EventType.RECEIVE_MESSAGE, (eventType, object) -> {
            metricMap.get(MqttMetricEnum.PACKETS_RECEIVED).getMetric().increment();
            if (object.getObject() instanceof MqttConnectMessage) {
                metricMap.get(MqttMetricEnum.PACKETS_CONNECT_RECEIVED).getMetric().increment();
            }
        });
        eventBus.subscribe(EventType.WRITE_MESSAGE, (eventType, object) -> {
            metricMap.get(MqttMetricEnum.PACKETS_SENT).getMetric().increment();
            if (object.getObject() instanceof MqttConnAckMessage) {
                metricMap.get(MqttMetricEnum.PACKETS_CONNACK_SENT).getMetric().increment();
            } else if (object.getObject() instanceof MqttPublishMessage) {
                switch (object.getObject().getFixedHeader().getQosLevel()) {
                    case AT_MOST_ONCE:
                        metricMap.get(MqttMetricEnum.MESSAGE_QOS0_SENT).getMetric().increment();
                        break;
                    case AT_LEAST_ONCE:
                        metricMap.get(MqttMetricEnum.MESSAGE_QOS1_SENT).getMetric().increment();
                        break;
                    case EXACTLY_ONCE:
                        metricMap.get(MqttMetricEnum.MESSAGE_QOS2_SENT).getMetric().increment();
                        break;
                }
            }
        });
        messageBusSubscriber.consumer((brokerContext1, publishMessage) -> {
            switch (publishMessage.getFixedHeader().getQosLevel()) {
                case AT_MOST_ONCE:
                    metricMap.get(MqttMetricEnum.MESSAGE_QOS0_RECEIVED).getMetric().increment();
                    break;
                case AT_LEAST_ONCE:
                    metricMap.get(MqttMetricEnum.MESSAGE_QOS1_RECEIVED).getMetric().increment();
                    break;
                case EXACTLY_ONCE:
                    metricMap.get(MqttMetricEnum.MESSAGE_QOS2_RECEIVED).getMetric().increment();
                    break;
            }

        });
    }

    private void initProvider() {
        //连接鉴权处理器
        providers.setConnectAuthenticationProvider(new ConfiguredConnectAuthenticationProviderImpl(this));
    }

    private final TopicSubscriber BREAK = new TopicSubscriber(null, null, null, 0, 0);

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

        pushTaskQueue = new ConcurrentLinkedQueue<>();
        executorService = Executors.newFixedThreadPool(2);
        executorService.execute(new AsyncTask() {
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
                        ConcurrentLinkedQueue<TopicSubscriber> subscribers = brokerTopic.getQueue();
                        AtomicInteger cnt = new AtomicInteger(subscribers.size());
                        subscribers.offer(BREAK);
                        TopicSubscriber subscriber = null;
                        int version = brokerTopic.getVersion().get();
                        while ((subscriber = subscribers.poll()) != null) {
                            if (subscriber == BREAK) {
                                break;
                            }

                            TopicSubscriber finalTs = subscriber;
                            pushTaskQueue.offer(() -> {
                                try {
                                    finalTs.batchPublish(BrokerContextImpl.this);
                                } finally {
                                    int val = cnt.decrementAndGet();
                                    if (val == 0 && version != brokerTopic.getVersion().get() && !subscribers.isEmpty()) {
                                        brokerTopic.getSemaphore().release();
                                        System.out.println("continue..." + brokerTopic.getTopic());
                                        notifyPush(brokerTopic);
                                    }
                                }
                            });
                        }
                        //brokerTopic.getSemaphore().release();
//                        if (version != brokerTopic.getVersion().get() && !subscribers.isEmpty()) {
//                            System.out.println("continue..." + brokerTopic.getTopic());
//                            notifyPush(brokerTopic);
//                        } else {
////                                System.out.println("empty...." + brokerTopic.getTopic());
//                        }
                    } catch (Exception e) {
                        LOGGER.error("brokerTopic:{} push message exception", brokerTopic.getTopic(), e);
                    }
                }
            }
        });


        for (int i = 0; i < getBrokerConfigure().getPushThreadNum(); i++) {
            pushThreadPool.execute(new AsyncTask() {
                @Override
                public void execute() {
                    while (true) {
                        try {
                            Runnable take = pushTaskQueue.poll();
                            if (take != null) {
                                take.run();
                            } else {
                                Thread.yield();
                            }
                        } catch (Exception e) {
                            LOGGER.error("pushTopicQueue exception", e);
                            break;
                        }
//                        BrokerTopic brokerTopic;
//                        try {
//                            brokerTopic = pushTopicQueue.take();
//
//                            int size = pushTopicQueue.size();
//                            if (size > 1024) {
//                                System.out.println("queue:" + size);
//                            }
//                            //Broker停止服务
//                            if (SHUTDOWN_TOPIC == brokerTopic) {
//                                pushTopicQueue.put(SHUTDOWN_TOPIC);
//                                break;
//                            }
//                        } catch (InterruptedException e) {
//                            LOGGER.error("pushTopicQueue exception", e);
//                            break;
//                        }
//                        try {
//                            //存在待输出消息
//                            ConcurrentLinkedQueue<TopicSubscriber> subscribers = brokerTopic.getQueue();
//                            subscribers.offer(BREAK);
//                            TopicSubscriber subscriber = null;
//                            int version = brokerTopic.getVersion().get();
//                            while ((subscriber = subscribers.poll()) != null) {
//                                if (subscriber == BREAK) {
//                                    break;
//                                }
//                                subscriber.batchPublish(BrokerContextImpl.this);
//                            }
//                            brokerTopic.getSemaphore().release();
//                            if (version != brokerTopic.getVersion().get() && !subscribers.isEmpty()) {
//                                System.out.println("continue..." + brokerTopic.getTopic());
//                                notifyPush(brokerTopic);
//                            } else {
////                                System.out.println("empty...." + brokerTopic.getTopic());
//                            }
//                        } catch (Exception e) {
//                            LOGGER.error("brokerTopic:{} push message exception", brokerTopic.getTopic(), e);
//                        }
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
                            BrokerTopic topic = subscriber.getTopic();
                            topic.getQueue().offer(subscriber);
                            notifyPush(topic);

                            //完成retain消息的消费，正式开始监听Topic
                            return;
                        }
                        //retain采用严格顺序publish模式
                        MqttSession session = subscriber.getMqttSession();

                        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(storedMessage.getPayload()).qos(subscriber.getMqttQoS()).topicName(storedMessage.getTopic());
                        if (subscriber.getMqttQoS() == MqttQoS.AT_LEAST_ONCE || subscriber.getMqttQoS() == MqttQoS.EXACTLY_ONCE) {
                            publishBuilder.packetId(session.newPacketId());
                        }
                        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
                            publishBuilder.publishProperties(new PublishProperties());
                        }
                        MqttPublishMessage publishMessage = publishBuilder.build();
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
            subscriber.getTopic().getQueue().offer(subscriber);
        });
    }

    void notifyPush(BrokerTopic topic) {
        if (!topic.getSemaphore().tryAcquire()) {
            return;
        }
        //已加入推送队列
        try {
            pushTopicQueue.put(topic);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

        if (StringUtils.isBlank(brokerConfigure.getHost())) {
            brokerConfigure.setHost("0.0.0.0");
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
            metric(MqttMetricEnum.TOPIC_COUNT).getMetric().increment();
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
    public Collection<MqttSession> getSessions() {
        return Collections.unmodifiableCollection(grantSessions.values());
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
    public BrokerRuntime getRuntime() {
        return runtime;
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
    public MetricItemTO metric(MqttMetricEnum metricEnum) {
        return metricMap.get(metricEnum);
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        eventBus.publish(ServerEventType.BROKER_DESTROY, this);
        messageBusExecutorService.shutdown();
        pushTopicQueue.offer(SHUTDOWN_TOPIC);
        pushThreadPool.shutdown();
        server.shutdown();
        asynchronousChannelGroup.shutdown();
        pagePool.release();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
