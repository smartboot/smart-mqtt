/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.KeepAliveMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBusSubscriber;
import org.smartboot.mqtt.broker.eventbus.messagebus.consumer.RetainPersistenceConsumer;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.processor.ConnectProcessor;
import org.smartboot.mqtt.broker.processor.DisConnectProcessor;
import org.smartboot.mqtt.broker.processor.MqttAckProcessor;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.processor.PingReqProcessor;
import org.smartboot.mqtt.broker.processor.PubRelProcessor;
import org.smartboot.mqtt.broker.processor.PublishProcessor;
import org.smartboot.mqtt.broker.processor.SubscribeProcessor;
import org.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import org.smartboot.mqtt.broker.provider.Providers;
import org.smartboot.mqtt.broker.topic.TopicPublishTree;
import org.smartboot.mqtt.broker.topic.TopicSubscribeTree;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.QosRetryPlugin;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;
import org.smartboot.socket.transport.AioQuickServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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
    private final TopicPublishTree topicPublishTree = new TopicPublishTree();

    private final TopicSubscribeTree subscribeTopicTree = new TopicSubscribeTree();
    /**
     * Keep-Alive监听线程
     */
    private final Timer timer = new HashedWheelTimer(r -> new Thread(r, "broker-timer"), 50, 1024);
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
    private final MqttBrokerMessageProcessor processor = new MqttBrokerMessageProcessor(this);

    //配置文件内容
    private String configJson;
    private final static BrokerTopic SHUTDOWN_TOPIC = new BrokerTopic("");

    private final Map<String, Object> resources = new Hashtable<>();
    private final Map<Class<? extends MqttMessage>, MqttProcessor<?>> processors;

    {
        Map<Class<? extends MqttMessage>, MqttProcessor<?>> mqttProcessors = new HashMap<>();
        mqttProcessors.put(MqttPingReqMessage.class, new PingReqProcessor());
        mqttProcessors.put(MqttConnectMessage.class, new ConnectProcessor());
        mqttProcessors.put(MqttPublishMessage.class, new PublishProcessor());
        mqttProcessors.put(MqttSubscribeMessage.class, new SubscribeProcessor());
        mqttProcessors.put(MqttUnsubscribeMessage.class, new UnSubscribeProcessor());
        mqttProcessors.put(MqttPubAckMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttPubRelMessage.class, new PubRelProcessor());
        mqttProcessors.put(MqttPubRecMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttPubCompMessage.class, new MqttAckProcessor<>());
        mqttProcessors.put(MqttDisconnectMessage.class, new DisConnectProcessor());
        processors = Collections.unmodifiableMap(mqttProcessors);
    }

    @Override
    public void init() throws IOException {

        updateBrokerConfigure();

        subscribeEventBus();

        subscribeMessageBus();

        initPushThread();

        loadAndInstallPlugins();


        try {
            brokerConfigure.addPlugin(new QosRetryPlugin());
            brokerConfigure.getPlugins().forEach(processor::addPlugin);
            server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(), new MqttProtocol(brokerConfigure.getMaxPacketSize()), processor);
            server.setBannerEnabled(false).setLowMemory(brokerConfigure.isLowMemory()).setReadBufferSize(brokerConfigure.getBufferSize()).setWriteBuffer(brokerConfigure.getBufferSize(), Math.min(brokerConfigure.getMaxInflight(), 16)).setBufferPagePool(brokerConfigure.getBufferPagePool()).setThreadNum(Math.max(2, brokerConfigure.getThreadNum()));
            server.start(brokerConfigure.getChannelGroup());
        } catch (Exception e) {
            destroy();
            throw e;
        }


        eventBus.publish(ServerEventType.BROKER_STARTED, this);
        //释放内存
        configJson = null;
        System.out.println(BrokerConfigure.BANNER + "\r\n :: smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");
        System.out.println("❤️Gitee: https://gitee.com/smartboot/smart-mqtt");
        System.out.println("Github: https://github.com/smartboot/smart-mqtt");
        System.out.println("Support: zhengjunweimail@163.com");
        if (StringUtils.isBlank(brokerConfigure.getHost())) {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [port:" + brokerConfigure.getPort() + "]");
        } else {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [host:" + brokerConfigure.getHost() + " port:" + brokerConfigure.getPort() + "]");
        }
    }


    private final TopicSubscriber BREAK = new TopicSubscriber();

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
                        try {
                            BrokerTopic brokerTopic = pushTopicQueue.take();

                            int size = pushTopicQueue.size();
                            if (size > 1024) {
                                System.out.println("queue:" + size);
                            }
                            //Broker停止服务
                            if (SHUTDOWN_TOPIC == brokerTopic) {
                                pushTopicQueue.put(SHUTDOWN_TOPIC);
                                break;
                            }
                            //存在待输出消息
                            ConcurrentLinkedQueue<TopicSubscriber> subscribers = brokerTopic.getQueue();
                            subscribers.offer(BREAK);
                            TopicSubscriber subscriber;
                            int preVersion = brokerTopic.getVersion().get();
                            while ((subscriber = subscribers.poll()) != BREAK) {
                                try {
                                    subscriber.batchPublish(BrokerContextImpl.this);
                                } catch (Exception e) {
                                    LOGGER.error("batch publish exception:{}", e.getMessage(), e);
                                }
                            }
                            brokerTopic.getSemaphore().release();
                            if (preVersion != brokerTopic.getVersion().get() && !subscribers.isEmpty()) {
                                notifyPush(brokerTopic);
                            }
                        } catch (InterruptedException e) {
                            LOGGER.error("pushTopicQueue exception", e);
                            break;
                        } catch (Exception e) {
                            LOGGER.error("push message exception", e);
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
        messageBusSubscriber.consumer(publishMessage -> providers.getPersistenceProvider().doSave(publishMessage));
        //消费retain消息
        messageBusSubscriber.consumer(new RetainPersistenceConsumer(this), Message::isRetained);
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
                messageBusSubscriber.consume(eventObject.getSession(), eventObject.getObject());
            } finally {
                eventBus.publish(ServerEventType.MESSAGE_BUS_CONSUMED, topic);
            }
        });

        //保持连接状态监听,长时间没有消息通信将断开连接
        eventBus.subscribe(ServerEventType.CONNECT, new KeepAliveMonitorSubscriber(this));
        //完成连接认证，移除监听器
        eventBus.subscribe(ServerEventType.CONNECT, (eventType, object) -> {
            MqttSession session = (MqttSession) object.getSession();
            session.idleConnectTimer.cancel();
            session.idleConnectTimer = null;
        });

        //消息总线消费完成，触发消息推送
        eventBus.subscribe(ServerEventType.MESSAGE_BUS_CONSUMED, (eventType, brokerTopic) -> {
            brokerTopic.getVersion().incrementAndGet();
            notifyPush(brokerTopic);
        });

        eventBus.subscribe(ServerEventType.NOTIFY_TOPIC_PUSH, (eventType, object) -> notifyPush(object));

        //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
        eventBus.subscribe(ServerEventType.SUBSCRIBE_TOPIC, (eventType, subscriber) -> retainPushThreadPool.execute(new AsyncTask() {
            @Override
            public void execute() {
                BrokerTopic topic = subscriber.getTopic();
                Message retainMessage = topic.getRetainMessage();
                if (retainMessage == null || retainMessage.getCreateTime() > subscriber.getLatestSubscribeTime()) {
                    topic.getQueue().offer(subscriber);
                    return;
                }
                MqttSession session = subscriber.getMqttSession();

                MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(retainMessage.getPayload()).qos(subscriber.getMqttQoS()).topicName(retainMessage.getTopic());
                if (session.getMqttVersion() == MqttVersion.MQTT_5) {
                    publishBuilder.publishProperties(new PublishProperties());
                }
                // Qos0不走飞行窗口
                if (subscriber.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
                    session.write(publishBuilder.build());
                    topic.getQueue().offer(subscriber);
                    return;
                }
                InflightQueue inflightQueue = session.getInflightQueue();
                // retain消息逐个推送
                CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = inflightQueue.offer(publishBuilder);
                future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
                    LOGGER.info("publish retain to client:{} success  ", session.getClientId());
                    topic.getQueue().offer(subscriber);
                });
                session.flush();
            }
        }));

        eventBus.subscribe(ServerEventType.TOPIC_CREATE, (eventType, object) -> subscribeTopicTree.match(object, (session, topicFilterSubscriber) -> {
            if (!providers.getSubscribeProvider().subscribeTopic(object.getTopic(), session)) {
                return;
            }
            session.subscribeSuccess(topicFilterSubscriber.getMqttQoS(), topicFilterSubscriber.getTopicFilterToken(), object);
        }));
    }

    private void notifyPush(BrokerTopic topic) {
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
        MqttUtil.updateConfig(brokerConfigure, "broker");

        brokerConfigure.setChannelGroup(new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            int i;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-mqtt-broker-" + (++i));
            }
        }));
        brokerConfigure.setBufferPagePool(new BufferPagePool(10 * 1024 * 1024, brokerConfigure.getThreadNum(), true));
        eventBus.publish(ServerEventType.BROKER_CONFIGURE_LOADED, brokerConfigure);
//        System.out.println("brokerConfigure: " + brokerConfigure);
    }

    /**
     * 加载并安装插件
     */
    private void loadAndInstallPlugins() {
        for (Plugin plugin : ServiceLoader.load(Plugin.class, Providers.class.getClassLoader())) {
            LOGGER.debug("load plugin: " + plugin.pluginName());
            plugins.add(plugin);
        }
        //安装插件
        plugins.stream().sorted(Comparator.comparingInt(Plugin::order)).forEach(plugin -> {
            LOGGER.debug("install plugin: " + plugin.pluginName());
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
            BrokerTopic newTopic = topicPublishTree.addTopic(topic);
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
    public Timer getTimer() {
        return timer;
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

    @Override
    public TopicPublishTree getPublishTopicTree() {
        return topicPublishTree;
    }

    @Override
    public TopicSubscribeTree getTopicSubscribeTree() {
        return subscribeTopicTree;
    }

    @Override
    public <T> void bundle(String key, T resource) {
        resources.put(key, resource);
    }

    @Override
    public <T> T getBundle(String key) {
        return (T) resources.get(key);
    }

    private void loadYamlConfig() throws IOException {
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
    public Map<Class<? extends MqttMessage>, MqttProcessor<?>> getMessageProcessors() {
        return processors;
    }


    @Override
    public void destroy() {
        LOGGER.info("destroy broker...");
        eventBus.publish(ServerEventType.BROKER_DESTROY, this);
        pushTopicQueue.offer(SHUTDOWN_TOPIC);
        pushThreadPool.shutdown();
        server.shutdown();
        brokerConfigure.getChannelGroup().shutdown();
        brokerConfigure.getBufferPagePool().release();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
