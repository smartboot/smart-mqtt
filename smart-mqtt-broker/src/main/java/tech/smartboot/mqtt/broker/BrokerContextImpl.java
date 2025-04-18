/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;
import org.smartboot.socket.transport.AioQuickServer;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.broker.bus.event.KeepAliveMonitorSubscriber;
import tech.smartboot.mqtt.broker.bus.message.RetainPersistenceConsumer;
import tech.smartboot.mqtt.broker.processor.ConnectProcessor;
import tech.smartboot.mqtt.broker.processor.DisConnectProcessor;
import tech.smartboot.mqtt.broker.processor.MqttAckProcessor;
import tech.smartboot.mqtt.broker.processor.PingReqProcessor;
import tech.smartboot.mqtt.broker.processor.PubRelProcessor;
import tech.smartboot.mqtt.broker.processor.PublishProcessor;
import tech.smartboot.mqtt.broker.processor.SubscribeProcessor;
import tech.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import tech.smartboot.mqtt.broker.provider.impl.session.MemorySessionStateProvider;
import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.broker.topic.BrokerTopicRegistry;
import tech.smartboot.mqtt.broker.topic.TopicSubscriptionRegistry;
import tech.smartboot.mqtt.broker.topic.deliver.AbstractMessageDeliver;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.InflightQueue;
import tech.smartboot.mqtt.common.MqttProtocol;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttDisconnectMessage;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import tech.smartboot.mqtt.common.message.MqttPingReqMessage;
import tech.smartboot.mqtt.common.message.MqttPubAckMessage;
import tech.smartboot.mqtt.common.message.MqttPubCompMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.PublishBuilder;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.provider.Providers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
 * MQTT Broker的核心实现类，负责管理整个Broker的生命周期和功能。
 * <p>
 * 该类实现了BrokerContext接口，提供了MQTT Broker的完整功能实现，包括：
 * <ul>
 *   <li>会话管理 - 维护客户端连接和认证状态</li>
 *   <li>主题管理 - 处理主题订阅和消息发布</li>
 *   <li>消息路由 - 确保消息准确投递到目标客户端</li>
 *   <li>事件处理 - 通过事件总线处理系统事件</li>
 *   <li>插件系统 - 支持功能扩展和定制化</li>
 * </ul>
 * </p>
 * <p>
 * 主要特性：
 * <ul>
 *   <li>高性能 - 采用异步IO和线程池优化性能</li>
 *   <li>可靠性 - 支持QoS等级和会话持久化</li>
 *   <li>可扩展 - 插件化架构便于功能扩展</li>
 *   <li>标准兼容 - 完整支持MQTT 3.1.1和MQTT 5.0协议</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * BrokerContext broker = new BrokerContextImpl();
 * broker.init(); // 初始化Broker
 * // ... 业务逻辑 ...
 * broker.destroy(); // 关闭Broker
 * </pre>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2018/4/26
 */
public class BrokerContextImpl implements BrokerContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerContextImpl.class);

    /**
     * 已通过认证的MQTT客户端会话映射。
     * <p>
     * Key为客户端标识符（ClientId），Value为对应的会话对象（MqttSession）。
     * 使用ConcurrentHashMap确保在多线程环境下的线程安全性。
     * 当客户端连接并完成认证后，其会话将被存储在此映射中。
     * </p>
     */
    private final ConcurrentMap<String, MqttSessionImpl> grantSessions = new ConcurrentHashMap<>();

    /**
     * 主题映射表，存储所有已创建的主题。
     * <p>
     * Key为主题名称，Value为对应的主题对象（BrokerTopic）。
     * 主题对象包含该主题的订阅者信息、消息队列和保留消息等。
     * </p>
     */
    private final ConcurrentMap<String, BrokerTopicImpl> topicMap = new ConcurrentHashMap<>();

    /**
     * Broker配置选项，包含服务器端口、最大连接数等配置参数。
     */
    private Options options;

    /**
     * 主题发布树，用于高效地管理和匹配消息发布。
     * <p>
     * 实现了基于树形结构的主题匹配算法，支持通配符（+和#）匹配。
     * 用于在消息发布时快速找到匹配的订阅者。
     * </p>
     */
    private final BrokerTopicRegistry topicRegistry = new BrokerTopicRegistry();

    /**
     * 主题订阅树，用于管理客户端的订阅关系。
     * <p>
     * 维护主题订阅的层级关系，支持：
     * <ul>
     *   <li>精确匹配 - 完全匹配主题名</li>
     *   <li>单层通配符(+) - 匹配单个层级</li>
     *   <li>多层通配符(#) - 匹配多个层级</li>
     * </ul>
     * </p>
     */
    private final TopicSubscriptionRegistry subscribeTopicTree = new TopicSubscriptionRegistry(this);

    /**
     * Keep-Alive定时器，用于监控客户端连接状态。
     * <p>
     * 基于HashedWheelTimer实现的高效定时器，用于：
     * <ul>
     *   <li>检测客户端是否保持活跃</li>
     *   <li>处理超时的MQTT消息</li>
     *   <li>清理过期的会话</li>
     * </ul>
     * </p>
     */
    private final Timer timer = new HashedWheelTimer(r -> new Thread(r, "broker-timer"), 50, 1024);

    /**
     * 消息总线，处理MQTT消息的内部传递和分发。
     * <p>
     * 提供了消息的异步处理和解耦，支持：
     * <ul>
     *   <li>消息的持久化</li>
     *   <li>保留消息的处理</li>
     *   <li>消息的重传和确认</li>
     * </ul>
     * </p>
     */
    private final MessageBusImpl messageBus = new MessageBusImpl(this);

    /**
     * 事件总线，用于处理Broker内部的事件通知。
     * <p>
     * 支持的事件类型包括：
     * <ul>
     *   <li>客户端连接/断开</li>
     *   <li>主题订阅/取消订阅</li>
     *   <li>消息发布/投递</li>
     *   <li>系统状态变更</li>
     * </ul>
     * </p>
     */
    private final EventBusImpl eventBus = new EventBusImpl();

    /**
     * 已加载的插件列表。
     * <p>
     * 存储所有已安装的插件实例，支持在运行时动态扩展Broker的功能。
     * 插件按照优先级顺序排序和执行。
     * </p>
     */
    private final List<Plugin> plugins = new ArrayList<>();

    /**
     * 服务提供者管理器，用于管理和获取各种服务实现。
     */
    private final Providers providers = new Providers();

    /**
     * 消息推送线程池，用于异步处理消息的推送任务。
     */
    private ExecutorService pushThreadPool;

    /**
     * 保留消息推送线程池，专门用于处理保留消息的异步推送。
     */
    private ExecutorService retainPushThreadPool;

    /**
     * MQTT Broker服务器实例。
     * <p>
     * 基于AIO实现的高性能网络服务器，负责：
     * <ul>
     *   <li>处理客户端连接</li>
     *   <li>解析MQTT协议包</li>
     *   <li>维护网络通信</li>
     * </ul>
     * </p>
     */
    private AioQuickServer server;

    /**
     * MQTT消息处理器，负责处理所有类型的MQTT协议消息。
     */
    private final MqttBrokerMessageProcessor processor = new MqttBrokerMessageProcessor(this);

    /**
     * Broker配置文件的JSON内容。
     */
    private String configJson;

    /**
     * MQTT消息处理器映射表。
     * <p>
     * Key为MQTT消息类型，Value为对应的消息处理器。
     * 支持所有MQTT协议定义的消息类型，包括：
     * <ul>
     *   <li>连接消息（CONNECT）</li>
     *   <li>发布消息（PUBLISH）</li>
     *   <li>订阅消息（SUBSCRIBE）</li>
     *   <li>确认消息（PUBACK等）</li>
     * </ul>
     * </p>
     */
    private final Map<Class<? extends MqttMessage>, MqttProcessor<?, ?, ?>> processors;
    private final BufferPagePool bufferPagePool = new BufferPagePool(1, true);

    {
        Map<Class<? extends MqttMessage>, MqttProcessor<?, ?, ?>> mqttProcessors = new HashMap<>();
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

    /**
     * 初始化MQTT Broker，完成所有必要的启动步骤。
     * <p>
     * 初始化过程包括：
     * <ul>
     *   <li>加载和更新Broker配置</li>
     *   <li>订阅系统事件和消息总线</li>
     *   <li>初始化消息推送线程池</li>
     *   <li>加载和安装插件</li>
     *   <li>启动网络服务器</li>
     * </ul>
     * </p>
     * <p>
     * 启动成功后会：
     * <ul>
     *   <li>发布Broker启动事件</li>
     *   <li>释放配置文件内存</li>
     *   <li>打印启动信息和版本号</li>
     * </ul>
     * </p>
     *
     * @throws Throwable 如果初始化过程中发生任何错误
     */
    public void init() throws Throwable {
        providers.setSessionStateProvider(new MemorySessionStateProvider());
        updateBrokerConfigure();

        subscribeEventBus();

        subscribeMessageBus();

        initPushThread();

        loadAndInstallPlugins();

        try {
            options.getPlugins().forEach(processor::addPlugin);
            server = new AioQuickServer(options.getHost(), options.getPort(), new MqttProtocol(options.getMaxPacketSize()), processor);
            server.setBannerEnabled(false).setReadBufferSize(options.getBufferSize()).setWriteBuffer(options.getBufferSize(), Math.min(options.getMaxInflight(), 16)).setBufferPagePool(bufferPagePool).setThreadNum(Math.max(2, options.getThreadNum()));
            if (!options.isLowMemory()) {
                server.disableLowMemory();
            }
            server.start(options.getChannelGroup());
        } catch (Exception e) {
            destroy();
            throw e;
        }

        eventBus.publish(EventType.BROKER_STARTED, this);
        //释放内存
        configJson = null;
        System.out.println(Options.BANNER + "\r\n ::smart-mqtt broker" + "::\t(" + Options.VERSION + ")");
        System.out.println("Gitee: https://gitee.com/smartboot/smart-mqtt");
        System.out.println("Github: https://github.com/smartboot/smart-mqtt");
        System.out.println("Document: https://smartboot.tech/smart-mqtt");
        System.out.println("Support: zhengjunweimail@163.com");
        if (StringUtils.isBlank(options.getHost())) {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [port:" + options.getPort() + "]");
        } else {
            System.out.println("\uD83C\uDF89start smart-mqtt success! [host:" + options.getHost() + " port:" + options.getPort() + "]");
        }
    }

    /**
     * 初始化消息推送线程池。
     * <p>
     * 创建两个线程池：
     * <ul>
     *   <li>retainPushThreadPool - 专门处理保留消息的推送</li>
     *   <li>pushThreadPool - 处理普通消息的推送</li>
     * </ul>
     * 线程池大小由配置参数pushThreadNum决定。
     * </p>
     * <p>
     * 同时设置主题限制：
     * <ul>
     *   <li>检查topicLimit配置</li>
     *   <li>如果未设置或小于等于0，默认设为10</li>
     * </ul>
     * </p>
     */
    private void initPushThread() {
        if (options.getTopicLimit() <= 0) {
            options.setTopicLimit(10);
        }
        retainPushThreadPool = Executors.newFixedThreadPool(Options().getPushThreadNum());
        pushThreadPool = Executors.newFixedThreadPool(Options().getPushThreadNum(), new ThreadFactory() {
            int index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-push-" + (index++));
            }
        });
    }

    /**
     * 订阅消息总线，设置消息处理器。
     * <p>
     * 主要处理两类消息：
     * <ul>
     *   <li>普通消息 - 根据订阅者数量处理消息分发：
     *     <ul>
     *       <li>获取或创建目标主题</li>
     *       <li>检查订阅者数量</li>
     *       <li>设置推送信号量</li>
     *       <li>将消息加入队列并触发推送</li>
     *     </ul>
     *   </li>
     *   <li>保留消息 - 使用专门的RetainPersistenceConsumer处理：
     *     <ul>
     *       <li>持久化保留消息</li>
     *       <li>管理消息的生命周期</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     */
    private void subscribeMessageBus() {
        //持久化消息
        messageBus.consumer((session, publishMessage) -> {
            BrokerTopicImpl brokerTopic = (BrokerTopicImpl) publishMessage.getTopic();
            int count = brokerTopic.subscribeCount();
            if (count == 0) {
                LOGGER.debug("none subscriber,ignore message");
            } else {
                publishMessage.setPushSemaphore(count);
                brokerTopic.getMessageQueue().put(publishMessage);
                brokerTopic.addVersion();
                brokerTopic.push();
            }
        });
        //消费retain消息
        messageBus.consumer(new RetainPersistenceConsumer(), Message::isRetained);
    }

    /**
     * 订阅事件总线，设置核心事件处理器。
     * <p>
     * 处理以下关键事件：
     * <ul>
     *   <li>连接事件（CONNECT）：
     *     <ul>
     *       <li>添加Keep-Alive监听器</li>
     *       <li>完成认证后移除连接监听</li>
     *     </ul>
     *   </li>
     *   <li>主题订阅事件（SUBSCRIBE_TOPIC）：
     *     <ul>
     *       <li>异步处理保留消息推送</li>
     *       <li>根据QoS级别处理消息投递</li>
     *       <li>维护订阅者状态</li>
     *     </ul>
     *   </li>
     *   <li>主题创建事件（TOPIC_CREATE）：
     *     <ul>
     *       <li>更新订阅树结构</li>
     *       <li>处理新主题的订阅关系</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     */
    private void subscribeEventBus() {
        //保持连接状态监听,长时间没有消息通信将断开连接
        eventBus.subscribe(EventType.CONNECT, new KeepAliveMonitorSubscriber(this));
        //完成连接认证，移除监听器
        eventBus.subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSessionImpl session = (MqttSessionImpl) object.getSession();
            session.idleConnectTimer.cancel();
            session.idleConnectTimer = null;
        });

        //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
        eventBus.subscribe(EventType.SUBSCRIBE_TOPIC, (eventType, eventObject) -> retainPushThreadPool.execute(new AsyncTask() {
            @Override
            public void execute() {
                AbstractMessageDeliver consumerRecord = (AbstractMessageDeliver) eventObject.getObject();
                BrokerTopicImpl topic = getOrCreateTopic(consumerRecord.getTopic().getTopic());
                Message retainMessage = topic.getRetainMessage();
                if (retainMessage == null || retainMessage.getCreateTime() > consumerRecord.getLatestSubscribeTime()) {
                    topic.addSubscriber(consumerRecord);
                    return;
                }
                MqttSessionImpl session = (MqttSessionImpl) (eventObject.getSession());

                PublishBuilder publishBuilder = PublishBuilder.builder().payload(retainMessage.getPayload()).qos(consumerRecord.getMqttQoS()).topic(retainMessage.getTopic());
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
                CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = inflightQueue.offer(publishBuilder);
                future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
                    LOGGER.info("publish retain to client:{} success  ", session.getClientId());
                    topic.addSubscriber(consumerRecord);
                });
                session.flush();
            }
        }));

        eventBus.subscribe(EventType.TOPIC_CREATE, (eventType, brokerTopic) -> subscribeTopicTree.refreshWhenTopicCreated(brokerTopic));
    }

    /**
     * 更新Broker配置信息。
     * <p>
     * 配置加载过程：
     * <ul>
     *   <li>加载YAML格式的配置文件：
     *     <ul>
     *       <li>优先使用外部配置文件</li>
     *       <li>默认使用classpath中的smart-mqtt.yaml</li>
     *     </ul>
     *   </li>
     *   <li>解析配置内容：
     *     <ul>
     *       <li>转换为JSON格式</li>
     *       <li>更新系统配置</li>
     *       <li>配置网络参数</li>
     *     </ul>
     *   </li>
     *   <li>发布配置加载完成事件</li>
     * </ul>
     * </p>
     *
     * @throws IOException 如果配置文件读取或解析失败
     */
    private void updateBrokerConfigure() throws IOException {
        //加载自定义配置文件
        loadYamlConfig();
        options = parseConfig("$.broker", Options.class);
        MqttUtil.updateConfig(options, "broker");
        options.setChannelGroup(new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            int i;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-mqtt-broker-" + (++i));
            }
        }));
        eventBus.publish(EventType.BROKER_CONFIGURE_LOADED, options);
//        System.out.println("brokerConfigure: " + brokerConfigure);
    }

    /**
     * 加载并安装插件，扩展Broker功能。
     * <p>
     * 插件管理流程：
     * <ul>
     *   <li>使用ServiceLoader机制加载插件：
     *     <ul>
     *       <li>自动发现classpath中的插件实现</li>
     *       <li>记录插件加载信息</li>
     *     </ul>
     *   </li>
     *   <li>插件安装：
     *     <ul>
     *       <li>按优先级排序插件</li>
     *       <li>依次调用插件的install方法</li>
     *       <li>初始化插件功能</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * @throws Throwable 如果插件加载或安装过程中发生错误
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
    public Options Options() {
        return options;
    }

    public void addSession(MqttSessionImpl session) {
        grantSessions.putIfAbsent(session.getClientId(), session);
    }

    @Override
    public BrokerTopicImpl getOrCreateTopic(String topic) {
        BrokerTopicImpl brokerTopic = topicMap.get(topic);
        if (brokerTopic == null) {
            synchronized (this) {
                brokerTopic = topicMap.get(topic);
                if (brokerTopic == null) {
                    ValidateUtils.isTrue(!MqttUtil.containsTopicWildcards(topic), "invalid topicName: " + topic);
                    brokerTopic = new BrokerTopicImpl(topic, options.getMaxMessageQueueLength(), pushThreadPool);
                    LOGGER.info("create topic: {} capacity is {}", topic, brokerTopic.getMessageQueue().capacity());
                    topicRegistry.registerTopic(brokerTopic);
                    topicMap.put(topic, brokerTopic);
                    eventBus.publish(EventType.TOPIC_CREATE, topic);
                }
            }
        }
        return brokerTopic;
    }

//    @Override
//    public Collection<BrokerTopic> getTopics() {
//        return topicMap.values();
//    }

    @Override
    public MessageBusImpl getMessageBus() {
        return messageBus;
    }

    @Override
    public EventBusImpl getEventBus() {
        return eventBus;
    }

    public MqttSession removeSession(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            LOGGER.warn("clientId is blank, ignore remove grantSession");
            return null;
        }
        return grantSessions.remove(clientId);
    }

    @Override
    public MqttSessionImpl getSession(String clientId) {
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

    public BrokerTopicRegistry getPublishTopicTree() {
        return topicRegistry;
    }

    public TopicSubscriptionRegistry getTopicSubscribeTree() {
        return subscribeTopicTree;
    }

    private void loadYamlConfig() throws IOException {
        String brokerConfig = StringUtils.defaultString(System.getProperty(Options.SystemProperty.BrokerConfig), System.getenv(Options.SystemProperty.BrokerConfig));

        InputStream inputStream;

        if (StringUtils.isBlank(brokerConfig)) {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("smart-mqtt.yaml");
            LOGGER.info("load smart-mqtt.yaml from classpath.");
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
    public Map<Class<? extends MqttMessage>, MqttProcessor<?, ?, ?>> getMessageProcessors() {
        return processors;
    }


    public void destroy() {
        LOGGER.info("destroy broker...");
        eventBus.publish(EventType.BROKER_DESTROY, this);
        topicMap.values().forEach(BrokerTopicImpl::disable);
        pushThreadPool.shutdown();
        if (server != null) {
            server.shutdown();
        }
        options.getChannelGroup().shutdown();
        timer.shutdown();

        bufferPagePool.release();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
