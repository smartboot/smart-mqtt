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
import org.smartboot.mqtt.broker.eventbus.EventBus;
import org.smartboot.mqtt.broker.eventbus.EventType;
import org.smartboot.mqtt.broker.eventbus.KeepAliveMonitorSubscriber;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
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
import org.smartboot.mqtt.broker.topic.BrokerTopic;
import org.smartboot.mqtt.broker.topic.MemoryMessageStoreQueue;
import org.smartboot.mqtt.broker.topic.TopicConsumerRecord;
import org.smartboot.mqtt.broker.topic.TopicPublishTree;
import org.smartboot.mqtt.broker.topic.TopicSubscribeTree;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.MqttProtocol;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private BrokerConfigure brokerConfigure;
    private final TopicPublishTree topicPublishTree = new TopicPublishTree();

    private final TopicSubscribeTree subscribeTopicTree = new TopicSubscribeTree();
    /**
     * Keep-Alive监听线程
     */
    private final Timer timer = new HashedWheelTimer(r -> new Thread(r, "broker-timer"), 50, 1024);
    private final MessageBus messageBusSubscriber = new MessageBus();
    /**
     * 事件总线
     */
    private final EventBus eventBus = new EventBus();
    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
    private ExecutorService pushThreadPool;
    private ExecutorService retainPushThreadPool;
    /**
     * Broker Server
     */
    private AioQuickServer server;
    private final MqttBrokerMessageProcessor processor = new MqttBrokerMessageProcessor(this);

    //配置文件内容
    private String configJson;

    private final Map<String, Object> resources = new Hashtable<>();
    private final Map<Class<? extends MqttMessage>, MqttProcessor<?>> processors;
    private final BufferPagePool bufferPagePool = new BufferPagePool(1, true);

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
    public void init() throws Throwable {

        updateBrokerConfigure();

        subscribeEventBus();

        subscribeMessageBus();

        initPushThread();

        loadAndInstallPlugins();


        try {
            brokerConfigure.getPlugins().forEach(processor::addPlugin);
            server = new AioQuickServer(brokerConfigure.getHost(), brokerConfigure.getPort(),
                    new MqttProtocol(brokerConfigure.getMaxPacketSize()), processor);
            server.setBannerEnabled(false).setReadBufferSize(brokerConfigure.getBufferSize()).setWriteBuffer(brokerConfigure.getBufferSize(), Math.min(brokerConfigure.getMaxInflight(), 16)).setBufferPagePool(bufferPagePool).setThreadNum(Math.max(2, brokerConfigure.getThreadNum()));
            if (!brokerConfigure.isLowMemory()) {
                server.disableLowMemory();
            }
            server.start(brokerConfigure.getChannelGroup());
        } catch (Exception e) {
            destroy();
            throw e;
        }


        eventBus.publish(EventType.BROKER_STARTED, this);
        //释放内存
        configJson = null;
        System.out.println(BrokerConfigure.BANNER + "\r\n ::smart-mqtt broker" + "::\t(" + BrokerConfigure.VERSION + ")");
        System.out.println("Gitee: https://gitee.com/smartboot/smart-mqtt");
        System.out.println("Github: https://github.com/smartboot/smart-mqtt");
        System.out.println("Document: https://smartboot.tech/smart-mqtt");
        System.out.println("Support: zhengjunweimail@163.com");
        if (StringUtils.isBlank(brokerConfigure.getHost())) {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [port:" + brokerConfigure.getPort() + "]");
        } else {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [host:" + brokerConfigure.getHost() + " port:" + brokerConfigure.getPort() + "]");
        }
    }

    private void initPushThread() {
        if (brokerConfigure.getTopicLimit() <= 0) {
            brokerConfigure.setTopicLimit(10);
        }
        retainPushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum());
        pushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum(), new ThreadFactory() {
            int index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-push-" + (index++));
            }
        });
    }

    /**
     * 订阅消息总线
     */
    private void subscribeMessageBus() {
        //持久化消息
        messageBusSubscriber.consumer((session, publishMessage) -> {
            BrokerTopic brokerTopic = getOrCreateTopic(publishMessage.getTopic());
            if (brokerTopic.isNoneSubscriber()) {
                LOGGER.debug("none subscriber,ignore message");
            } else {
                brokerTopic.getMessageQueue().put(publishMessage);
                brokerTopic.addVersion();
                brokerTopic.push();
            }
        });
        //消费retain消息
        messageBusSubscriber.consumer(new RetainPersistenceConsumer(this), Message::isRetained);
    }

    /**
     * 订阅事件总线
     */
    private void subscribeEventBus() {
        //保持连接状态监听,长时间没有消息通信将断开连接
        eventBus.subscribe(EventType.CONNECT, new KeepAliveMonitorSubscriber(this));
        //完成连接认证，移除监听器
        eventBus.subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();
            session.idleConnectTimer.cancel();
            session.idleConnectTimer = null;
        });

        //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
        eventBus.subscribe(EventType.SUBSCRIBE_TOPIC,
                (eventType, eventObject) -> retainPushThreadPool.execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        TopicConsumerRecord consumerRecord = eventObject.getObject();
                        BrokerTopic topic = consumerRecord.getTopic();
                        Message retainMessage = topic.getRetainMessage();
                        if (retainMessage == null || retainMessage.getCreateTime() > consumerRecord.getLatestSubscribeTime()) {
                            topic.addSubscriber(consumerRecord);
                            return;
                        }
                        MqttSession session = eventObject.getSession();

                        MqttMessageBuilders.PublishBuilder publishBuilder =
                                MqttMessageBuilders.publish().payload(retainMessage.getPayload()).qos(consumerRecord.getMqttQoS()).topic(retainMessage.getTopicBytes());
                        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
                            publishBuilder.publishProperties(new PublishProperties());
                        }
                        // Qos0不走飞行窗口
                        if (consumerRecord.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
                            session.write(publishBuilder.build());
                            topic.addSubscriber(consumerRecord);
                            return;
                        }
                        InflightQueue inflightQueue = session.getInflightQueue();
                        // retain消息逐个推送
                        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future =
                                inflightQueue.offer(publishBuilder);
                        future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
                            LOGGER.info("publish retain to client:{} success  ", session.getClientId());
                            topic.addSubscriber(consumerRecord);
                        });
                        session.flush();
                    }
                }));

        eventBus.subscribe(EventType.TOPIC_CREATE,
                (eventType, brokerTopic) -> subscribeTopicTree.refreshMatchRelation(brokerTopic, (session,
                                                                                                  topicFilterSubscriber) -> {
                    if (!providers.getSubscribeProvider().subscribeTopic(brokerTopic.getTopic(), session)) {
                        return;
                    }
                    session.subscribeSuccess(topicFilterSubscriber, brokerTopic);
                }));
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
        eventBus.publish(EventType.BROKER_CONFIGURE_LOADED, brokerConfigure);
//        System.out.println("brokerConfigure: " + brokerConfigure);
    }

    /**
     * 加载并安装插件
     */
    private void loadAndInstallPlugins() throws Throwable {
        for (Plugin plugin : ServiceLoader.load(Plugin.class, Providers.class.getClassLoader())) {
            LOGGER.debug("load plugin: " + plugin.pluginName());
            plugins.add(plugin);
        }
        //安装插件
        plugins.sort(Comparator.comparingInt(Plugin::order));
        for (Plugin plugin : plugins) {
            LOGGER.debug("install plugin: " + plugin.pluginName());
            plugin.install(this);
        }
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
        BrokerTopic brokerTopic = topicMap.get(topic);
        if (brokerTopic == null) {
            synchronized (this) {
                brokerTopic = topicMap.get(topic);
                if (brokerTopic == null) {
                    ValidateUtils.isTrue(!MqttUtil.containsTopicWildcards(topic), "invalid topicName: " + topic);
                    brokerTopic = new BrokerTopic(topic,
                            new MemoryMessageStoreQueue(brokerConfigure.getMaxMessageQueueLength()), pushThreadPool);
                    LOGGER.info("create topic: {} capacity is {}", topic, brokerTopic.getMessageQueue().capacity());
                    topicPublishTree.addTopic(brokerTopic);
                    eventBus.publish(EventType.TOPIC_CREATE, brokerTopic);
                    topicMap.put(topic, brokerTopic);
                }
            }
        }
        return brokerTopic;
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
        String brokerConfig =
                StringUtils.defaultString(System.getProperty(BrokerConfigure.SystemProperty.BrokerConfig),
                        System.getenv(BrokerConfigure.SystemProperty.BrokerConfig));

        InputStream inputStream;

        if (StringUtils.isBlank(brokerConfig)) {
            inputStream = BrokerContext.class.getClassLoader().getResourceAsStream("smart-mqtt.yaml");
            LOGGER.debug("load smart-mqtt.yaml from classpath.");
        } else {
            inputStream = Files.newInputStream(Paths.get(brokerConfig));
            LOGGER.debug("load external yaml config.");
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
        eventBus.publish(EventType.BROKER_DESTROY, this);
        topicMap.values().forEach(BrokerTopic::disable);
        pushThreadPool.shutdown();
        if (server != null) {
            server.shutdown();
        }
        brokerConfigure.getChannelGroup().shutdown();
        timer.shutdown();

        bufferPagePool.release();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
