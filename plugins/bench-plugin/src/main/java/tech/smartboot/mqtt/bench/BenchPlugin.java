/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.bench;

import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.schema.Enum;
import tech.smartboot.mqtt.plugin.spec.schema.Item;
import tech.smartboot.mqtt.plugin.spec.schema.Schema;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT压测插件
 * 支持Publish和Subscribe两种压测场景
 *
 * @author 三刀
 * @version v1.5.1
 */
public class BenchPlugin extends Plugin {

    private static final String SCENARIO_PUBLISH = "publish";
    private static final String SCENARIO_SUBSCRIBE = "subscribe";

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Semaphore semaphore = new Semaphore(1);

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        brokerContext.getEventBus().subscribe(EventType.BROKER_STARTED, new DisposableEventBusSubscriber<BrokerContext>() {
            @Override
            public void consumer(EventType<BrokerContext> eventType, BrokerContext object) {
                init(brokerContext);
            }
        });
        init(brokerContext);
    }

    private void init(BrokerContext brokerContext) {
        if (!semaphore.tryAcquire()) {
            return;
        }

        PluginConfig config = loadPluginConfig(PluginConfig.class);

        String scenario = config.getScenario();
        System.out.println("[bench-plugin] 压测场景: " + scenario);

        running.set(true);

        if (SCENARIO_PUBLISH.equals(scenario)) {
            runPublishBenchmark(brokerContext, config.getPublish());
        } else if (SCENARIO_SUBSCRIBE.equals(scenario)) {
            runSubscribeBenchmark(brokerContext, config.getSubscribe());
        } else {
            System.out.println("[bench-plugin] 未知场景: " + scenario + ", 支持: publish, subscribe");
        }
    }


    @Override
    protected void destroyPlugin() {
        System.out.println("[bench-plugin] 开始优雅关闭...");
        running.set(false);
    }

    /**
     * 运行发布压测
     */
    private void runPublishBenchmark(BrokerContext brokerContext, PublishConfig config) {
        int connections = config.getConnections();
        int payloadSize = config.getPayloadSize();
        int topicCount = config.getTopicCount();
        int publishCount = config.getPublishCount();
        int period = config.getPeriod();
        int qos = config.getQos();

        System.out.println("[bench-plugin] 启动发布压测:");
        System.out.println("  连接数: " + connections);
        System.out.println("  负载大小: " + payloadSize + " bytes");
        System.out.println("  主题数: " + topicCount);
        System.out.println("  每次发布数: " + publishCount);
        System.out.println("  发布间隔: " + period + "ms");
        System.out.println("  QoS: " + qos);


        // 创建发布任务
        AtomicInteger topicIndex = new AtomicInteger();
        byte[] payload = new byte[payloadSize];
        Arrays.fill(payload, (byte) 1);

        // 创建连接
        for (int i = 0; i < connections; i++) {
            final int clientId = i;
            MqttClient client = new MqttClient(config.getHost(), config.getPort(), opt -> opt.setGroup(brokerContext.Options().getChannelGroup()).setKeepAliveInterval(30).setAutomaticReconnect(true).setClientId("bench-pub-" + clientId));

            client.connect(mqttConnAckMessage -> {
                Thread publishThread = new Thread(() -> {
                    try {
                        while (running.get()) {
                            try {
                                for (int j = 0; j < publishCount; j++) {
                                    String topic = "/topic" + (topicIndex.incrementAndGet() % topicCount);
                                    client.publish(topic, MqttQoS.valueOf(qos), payload, false, false);
                                }
                                client.flush();
                                Thread.sleep(period);
                            } catch (InterruptedException e) {
                                break;
                            } catch (Exception e) {
                                System.err.println("[bench-plugin] 发布异常: " + e.getMessage());
                            }
                        }
                    } finally {
                        client.disconnect();
                    }
                }, "bench-publish-" + hashCode());
                publishThread.start();
            });
        }
    }

    /**
     * 运行订阅压测
     */
    private void runSubscribeBenchmark(BrokerContext brokerContext, SubscribeConfig config) {
        int connections = config.getConnections();
        int topicCount = config.getTopicCount();
        int qos = config.getQos();

        System.out.println("[bench-plugin] 启动订阅压测:");
        System.out.println("  连接数: " + connections);
        System.out.println("  主题数: " + topicCount);
        System.out.println("  QoS: " + qos);

        // 创建测试主题
        long random = System.currentTimeMillis() % 10000;
        for (int j = 0; j < topicCount; j++) {
            String topicName = "topic_" + random + "_" + j;
            brokerContext.getOrCreateTopic(topicName);
        }
        System.out.println("[bench-plugin] 测试主题已创建");

        ConcurrentLinkedQueue<MqttClient> clients = new ConcurrentLinkedQueue<>();
        // 创建订阅连接
        for (int i = 0; i < connections; i++) {
            final int clientId = i;
            MqttClient client = new MqttClient(config.getHost(), config.getPort(), opt -> opt.setGroup(brokerContext.Options().getChannelGroup()).setKeepAliveInterval(30).setAutomaticReconnect(true).setClientId("bench-sub-" + clientId));
            clients.offer(client);

            client.connect(mqttConnAckMessage -> {
                // 连接成功后订阅主题
                for (int j = 0; j < topicCount; j++) {
                    String topicName = "topic_" + random + "_" + j;
                    client.subscribe(topicName, MqttQoS.valueOf(qos), (mqttClient, message) -> {
                        // 收到消息的回调
                    });
                }
            });
        }

        // 创建发布者来触发消息
        int publisherCount = config.getPublisherCount();
        int publishCount = config.getPublishCount();
        int publishPeriod = config.getPublishPeriod();

        System.out.println("[bench-plugin] 启动发布者数量: " + publisherCount);


        AtomicInteger pubTopicIndex = new AtomicInteger();
        byte[] payload = new byte[config.getPayloadSize()];
        Arrays.fill(payload, (byte) 1);

        // 创建发布者线程
        for (int i = 0; i < publisherCount; i++) {
            final int pubId = i;
            MqttClient publisher = new MqttClient(config.getHost(), config.getPort(), opt -> opt.setGroup(brokerContext.Options().getChannelGroup()).setKeepAliveInterval(30).setAutomaticReconnect(true).setClientId("bench-publisher-" + pubId));

            publisher.connect(mqttConnAckMessage -> {
                Thread publisherThread = new Thread(() -> {
                    try {
                        while (running.get()) {
                            try {
                                for (int j = 0; j < publishCount; j++) {
                                    String topic = "topic_" + random + "_" + (pubTopicIndex.incrementAndGet() % topicCount);
                                    publisher.publish(topic, MqttQoS.AT_MOST_ONCE, payload, false, false);
                                }
                                publisher.flush();
                                Thread.sleep(publishPeriod);
                            } catch (Exception e) {
                                System.err.println("[bench-plugin] 发布者异常: " + e.getMessage());
                            }
                        }
                    } finally {
                        publisher.disconnect();
                        MqttClient client = clients.poll();
                        while (client != null) {
                            client.disconnect();
                            client = clients.poll();
                        }
                    }
                }, "bench-publisher-" + pubId);
                publisherThread.start();
            });
        }
    }

    @Override
    public String getVersion() {
        return Options.VERSION;
    }

    @Override
    public String getVendor() {
        return Options.VENDOR;
    }

    @Override
    public Schema schema() {
        Schema schema = new Schema();

        // 主配置
        Item scenarioItem = Item.String("scenario", "压测场景").tip("publish: 发布压测, subscribe: 订阅压测").addEnums(Enum.of("publish", "发布压测"), Enum.of("subscribe", "订阅压测"));
        schema.addItem(scenarioItem);

        // Publish配置
        Item publishItem = Item.Object("publish", "发布压测配置").col(6);
        publishItem.addItems(Item.String("host", "MQTT服务器地址").tip("默认: 127.0.0.1"), Item.Int("port", "MQTT服务器端口").tip("默认: 1883"), Item.Int("connections", "并发连接数").tip("默认: 1000"), Item.Int("payloadSize", "消息负载大小(字节)").tip("默认: 1024"), Item.Int("topicCount", "主题数量").tip("默认: 128"), Item.Int("publishCount", "每次发布数").tip("默认: 1"), Item.Int("period", "发布间隔(毫秒)").tip("默认: 1"), Item.Int("qos", "QoS等级").tip("0: AtMostOnce, 1: AtLeastOnce, 2: ExactlyOnce"));
        schema.addItem(publishItem);

        // Subscribe配置
        Item subscribeItem = Item.Object("subscribe", "订阅压测配置").col(6);
        subscribeItem.addItems(Item.String("host", "MQTT服务器地址").tip("默认: 127.0.0.1"), Item.Int("port", "MQTT服务器端口").tip("默认: 1883"), Item.Int("connections", "并发连接数").tip("默认: 1000"), Item.Int("topicCount", "主题数量").tip("默认: 128"), Item.Int("qos", "QoS等级").tip("0: AtMostOnce, 1: AtLeastOnce, 2: ExactlyOnce"), Item.Int("publisherCount", "发布者数量").tip("0: 不启动发布者, 默认: 1"), Item.Int("publishCount", "每次发布数").tip("默认: 1"), Item.Int("publishPeriod", "发布间隔(毫秒)").tip("默认: 1"), Item.Int("payloadSize", "消息负载大小(字节)").tip("默认: 128"));
        schema.addItem(subscribeItem);

        return schema;
    }

    @Override
    public String pluginName() {
        return "bench-plugin";
    }
}