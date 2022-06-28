package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.listener.BrokerLifecycleListener;
import org.smartboot.mqtt.broker.listener.BrokerListeners;
import org.smartboot.mqtt.broker.listener.TopicEventListener;
import org.smartboot.mqtt.broker.listener.impl.ConnectIdleTimeListener;
import org.smartboot.mqtt.broker.listener.impl.MessageLoggerListener;
import org.smartboot.mqtt.broker.messagebus.Message;
import org.smartboot.mqtt.broker.messagebus.MessageBus;
import org.smartboot.mqtt.broker.messagebus.MessageBusImpl;
import org.smartboot.mqtt.broker.messagebus.subscribe.PersistenceAndPushSubscriber;
import org.smartboot.mqtt.broker.messagebus.subscribe.RetainPersistenceSubscriber;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.provider.Providers;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.listener.MqttSessionListener;
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

    /**
     * ACK超时监听
     */
    private final ScheduledExecutorService ACK_TIMEOUT_MONITOR_EXECUTOR = Executors.newSingleThreadScheduledExecutor();


    private final MessageBus messageBus = new MessageBusImpl();

    private final List<Plugin> plugins = new ArrayList<>();
    private final Providers providers = new Providers();
    private final BrokerListeners listeners = new BrokerListeners();
    /**
     * Broker Server
     */
    private AioQuickServer server;

    @Override
    public void init() throws IOException {
        pushThreadPool = Executors.newFixedThreadPool(getBrokerConfigure().getPushThreadNum());
        updateBrokerConfigure();
        //注册Listener
        addEvent(new ConnectIdleTimeListener(brokerConfigure));
        addEvent(new MessageLoggerListener());

        //消费retain消息
        getMessageBus().subscribe(new RetainPersistenceSubscriber(this), Message::isRetained);

        //消息持久化
        getMessageBus().subscribe(new PersistenceAndPushSubscriber(this));

        // 推送消息
//        getMessageBus().subscribe(new PushTriggerSubscriber(this));

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

        System.out.println("brokerConfigure: " + brokerConfigure);
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
    public MessageBus getMessageBus() {
        return messageBus;
    }

    @Override
    public MqttSession removeSession(String clientId) {
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

    public void batchPublish(String topicName) {
        BrokerTopic topic = getOrCreateTopic(topicName);
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
        listeners.getBrokerLifecycleListeners().forEach(listener -> listener.onDestroy(this));
        server.shutdown();
    }
}
