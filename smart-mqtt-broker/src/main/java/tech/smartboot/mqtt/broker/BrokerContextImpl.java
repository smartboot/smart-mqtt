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

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;
import org.smartboot.socket.transport.AioQuickServer;
import tech.smartboot.mqtt.broker.bus.event.KeepAliveMonitorSubscriber;
import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.common.MqttProtocol;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.BrokerTopic;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.PluginRegistry;
import tech.smartboot.mqtt.plugin.spec.bus.EventBus;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBus;
import tech.smartboot.mqtt.plugin.spec.provider.Providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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
     * 主题的保留消息。
     * <p>
     * 符合MQTT协议的保留消息机制，新订阅者会立即收到该主题的最新保留消息。
     * 保留消息可以通过发布新消息更新，或通过发布空消息删除。
     * </p>
     */
    private final Map<String, RetainMessage> retains = new ConcurrentHashMap<>();

    /**
     * Broker配置选项，包含服务器端口、最大连接数等配置参数。
     */
    private final Options options = new Options();

    /**
     * 主题发布树，用于高效地管理和匹配消息发布。
     * <p>
     * 实现了基于树形结构的主题匹配算法，支持通配符（+和#）匹配。
     * 用于在消息发布时快速找到匹配的订阅者。
     * </p>
     */
    private final BrokerTopicMatcher topicMatcher = new BrokerTopicMatcher(null);

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
    private final SubscribeRelationMatcher subscribeTopicTree = new SubscribeRelationMatcher();

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
     * InflightQueue定时器，用于处理MQTT消息的QoS等级。
     * <p>
     * 基于HashedWheelTimer实现的高效定时器，用于：
     * <ul>
     *   <li>处理QoS等级为1的消息的ACK确认</li>
     *   <li>处理QoS等级为2的消息的ACK确认</li>
     *   <li>处理QoS等级为2的消息的REC/PUB消息处理</li>
     * </ul>
     * </p>
     */
    private final Timer inflightQueueTimer = new HashedWheelTimer(r -> new Thread(r, "broker-inflight-timer"), 50, 1024);

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
    private final MessageBusImpl messageBus = new MessageBusImpl();

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


    private final PluginRegistryImpl pluginRegistry = new PluginRegistryImpl(this);

    private BufferPagePool bufferPagePool;


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
        long start = System.currentTimeMillis();
        updateBrokerConfigure();

        subscribeEventBus();

        subscribeMessageBus();

        initPushThread();

        loadAndInstallPlugins();

        pluginRegistry.init();

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
        System.out.println(Options.BANNER + "\r\n ::smart-mqtt broker" + "::\t(" + Options.VERSION + ")");
        System.out.println("Gitee: https://gitee.com/smartboot/smart-mqtt");
        System.out.println("Github: https://github.com/smartboot/smart-mqtt");
        System.out.println("Document: https://smartboot.tech/smart-mqtt");
        System.out.println("Support: zhengjunweimail@163.com");
        if (options.getHost() == null || options.getHost().isEmpty()) {
            System.out.println("\uD83C\uDF89start smart-mqtt success, cost: " + (System.currentTimeMillis() - start) + "ms ! [port:" + options.getPort() + "]");
        } else {
            System.out.println("\uD83C\uDF89start smart-mqtt success, cost: " + (System.currentTimeMillis() - start) + "ms ! [host:" + options.getHost() + " port:" + options.getPort() + "]");
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
        pushThreadPool = Executors.newFixedThreadPool(Options().getPushThreadNum(), new ThreadFactory() {
            int index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-pusher-" + (index++));
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
            if (count > 0) {
                publishMessage.setPushSemaphore(count);
                brokerTopic.getMessageQueue().put(publishMessage);
                brokerTopic.addVersion();
                brokerTopic.push();
            }
        });
        //消费retain消息
        messageBus.consumer((session, message) -> {
            BrokerTopic topic = message.getTopic();
            //保留标志为 1 且有效载荷为零字节的 PUBLISH 报文会被服务端当作正常消息处理，它会被发送给订阅主题匹配的客户端。
            // 此外，同一个主题下任何现存的保留消息必须被移除，因此这个主题之后的任何订阅者都不会收到一个保留消息。
            if (message.getPayload().length == 0) {
//                LOGGER.info("clear topic:{} retained messages, because of current retained message's payload length is 0", topic.getTopic());
                retains.remove(topic.getTopic());
                return;
            }
            /*
             * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
             * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
             * 如果这种情况发生了，那个主题将没有保留消息
             */
            if (message.getQos() == MqttQoS.AT_MOST_ONCE) {
//                LOGGER.info("receive Qos0 retain message,clear topic:{} retained messages", topic.getTopic());
                retains.remove(topic.getTopic());
            }
            retains.put(topic.getTopic(), new RetainMessage(message.getPayload(), topic.getTopic()));
        }, Message::isRetained);
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

        eventBus.subscribe(EventType.TOPIC_CREATE, (eventType, brokerTopic) -> subscribeTopicTree.match(getOrCreateTopic(brokerTopic)));
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
        updateOptions();
        System.out.println("Broker Options: " + options);
        options.setChannelGroup(new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            int i;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-mqtt-broker-" + (++i));
            }
        }));
        options.setProcessor(processor);
        this.bufferPagePool = new BufferPagePool(Runtime.getRuntime().availableProcessors(), true);
        eventBus.publish(EventType.BROKER_CONFIGURE_LOADED, options);
//        System.out.println("brokerConfigure: " + brokerConfigure);
    }

    /**
     * 静态代码的私有化实现，用于更新配置，支持native编译
     *
     */
    private void updateOptions() {
        try {

            // 处理host配置
            String hostValue = getPropertyValue("host");
            if (hostValue != null) {
                options.setHost(hostValue);
            }

            // 处理port配置
            String portValue = getPropertyValue("port");
            if (portValue != null) {
                try {
                    options.setPort(Integer.parseInt(portValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid port value: " + portValue, e);
                }
            }

            // 处理threadNum配置
            String threadNumValue = getPropertyValue("threadNum");
            if (threadNumValue != null) {
                try {
                    options.setThreadNum(Integer.parseInt(threadNumValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid threadNum value: " + threadNumValue, e);
                }
            }

            // 处理bufferSize配置
            String bufferSizeValue = getPropertyValue("bufferSize");
            if (bufferSizeValue != null) {
                try {
                    options.setBufferSize(Integer.parseInt(bufferSizeValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid bufferSize value: " + bufferSizeValue, e);
                }
            }

            // 处理maxInflight配置
            String maxInflightValue = getPropertyValue("maxInflight");
            if (maxInflightValue != null) {
                try {
                    options.setMaxInflight(Integer.parseInt(maxInflightValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid maxInflight value: " + maxInflightValue, e);
                }
            }

            // 处理maxPacketSize配置
            String maxPacketSizeValue = getPropertyValue("maxPacketSize");
            if (maxPacketSizeValue != null) {
                try {
                    options.setMaxPacketSize(Integer.parseInt(maxPacketSizeValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid maxPacketSize value: " + maxPacketSizeValue, e);
                }
            }

            // 处理pushThreadNum配置
            String pushThreadNumValue = getPropertyValue("pushThreadNum");
            if (pushThreadNumValue != null) {
                try {
                    options.setPushThreadNum(Integer.parseInt(pushThreadNumValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid pushThreadNum value: " + pushThreadNumValue, e);
                }
            }

            // 处理topicLimit配置
            String topicLimitValue = getPropertyValue("topicLimit");
            if (topicLimitValue != null) {
                try {
                    options.setTopicLimit(Integer.parseInt(topicLimitValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid topicLimit value: " + topicLimitValue, e);
                }
            }

            // 处理maxMessageQueueLength配置
            String maxMessageQueueLengthValue = getPropertyValue("maxMessageQueueLength");
            if (maxMessageQueueLengthValue != null) {
                try {
                    options.setMaxMessageQueueLength(Integer.parseInt(maxMessageQueueLengthValue));
                } catch (NumberFormatException e) {
                    throw new MqttException("Invalid maxMessageQueueLength value: " + maxMessageQueueLengthValue, e);
                }
            }

            // 处理lowMemory配置
            String lowMemoryValue = getPropertyValue("lowMemory");
            if (lowMemoryValue != null) {
                options.setLowMemory(Boolean.parseBoolean(lowMemoryValue));
            }

        } catch (Throwable throwable) {
            throw new MqttException("update config exception", throwable);
        }
    }

    /**
     * 获取配置属性值，优先级：系统属性 > 环境变量
     *
     * @param fieldName 字段名
     * @return 配置值，如果未设置则返回null
     */
    private static String getPropertyValue(String fieldName) {
        // 系统属性优先
        String value = System.getProperty("broker" + "." + fieldName);
        // 环境属性次之
        if (value == null) {
            value = System.getenv(("broker" + "." + fieldName).replace(".", "_").toUpperCase());
        }
        return value;
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
        for (Plugin plugin : ServiceLoader.load(Plugin.class, BrokerContextImpl.class.getClassLoader())) {
            System.out.println("load plugin: " + plugin.pluginName());
            plugins.add(plugin);
        }
        //安装插件
        plugins.sort(Comparator.comparingInt(Plugin::order));
        for (Plugin plugin : plugins) {
            System.out.println("install plugin: " + plugin.pluginName());
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
                    topicMatcher.add(brokerTopic);
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
    public MessageBus getMessageBus() {
        return messageBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public MqttSession removeSession(String clientId) {
        if (MqttUtil.isBlank(clientId)) {
            System.err.println("clientId is blank, ignore remove grantSession");
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

    RetainMessage getRetain(String topic) {
        return retains.get(topic);
    }

    public Timer getInflightQueueTimer() {
        return inflightQueueTimer;
    }

    @Override
    public Providers getProviders() {
        return providers;
    }

    BrokerTopicMatcher getTopicMatcher() {
        return topicMatcher;
    }

    SubscribeRelationMatcher getRelationMatcher() {
        return subscribeTopicTree;
    }

    @Override
    public PluginRegistry pluginRegistry() {
        return pluginRegistry;
    }

    @Override
    public BufferPagePool bufferPagePool() {
        return bufferPagePool;
    }

    public void destroy() {
        eventBus.publish(EventType.BROKER_DESTROY, this);
        topicMap.values().forEach(BrokerTopicImpl::disable);
        retains.clear();

        pushThreadPool.shutdown();
        if (server != null) {
            server.shutdown();
        }
        options.getChannelGroup().shutdown();
        timer.shutdown();
        inflightQueueTimer.shutdown();

        bufferPagePool.release();
        pluginRegistry.destroy();
        //卸载插件
        plugins.forEach(Plugin::uninstall);
        plugins.clear();
    }
}
