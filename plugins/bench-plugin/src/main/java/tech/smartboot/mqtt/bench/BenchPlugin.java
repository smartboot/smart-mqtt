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

import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.BrokerTopic;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        PluginConfig config = loadPluginConfig(PluginConfig.class);

        String scenario = config.getScenario();
        System.out.println("[bench-plugin] 压测场景: " + scenario);

        if (SCENARIO_PUBLISH.equals(scenario)) {
            runPublishBenchmark(config.getPublish());
        } else if (SCENARIO_SUBSCRIBE.equals(scenario)) {
            runSubscribeBenchmark(brokerContext, config.getSubscribe());
        } else {
            System.out.println("[bench-plugin] 未知场景: " + scenario + ", 支持: publish, subscribe");
        }
    }

    /**
     * 运行发布压测
     */
    private void runPublishBenchmark(PublishConfig config) throws Exception {
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

        // 等待Broker启动
        Thread.sleep(1000);

        AsynchronousChannelGroup channelGroup = new EnhanceAsynchronousChannelProvider(
                Boolean.parseBoolean(System.getenv("BROKER_LOWMEMORY")))
                .openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(),
                        r -> new Thread(r, "bench-publish-pool"));

        MqttClient[] clients = new MqttClient[connections];
        CountDownLatch latch = new CountDownLatch(connections);

        // 创建连接
        for (int i = 0; i < connections; i++) {
            final int clientId = i;
            MqttClient client = new MqttClient(config.getHost(), config.getPort(),
                    opt -> opt.setGroup(channelGroup)
                            .setKeepAliveInterval(30)
                            .setAutomaticReconnect(true)
                            .setClientId("bench-pub-" + clientId));

            client.connect(mqttConnAckMessage -> {
                clients[clientId] = client;
                latch.countDown();
            });
        }

        latch.await();
        System.out.println("[bench-plugin] 所有连接已建立");

        // 创建发布任务
        AtomicInteger topicIndex = new AtomicInteger();
        byte[] payload = new byte[payloadSize];
        Arrays.fill(payload, (byte) 1);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors());

        for (MqttClient client : clients) {
            executor.scheduleWithFixedDelay(() -> {
                try {
                    for (int j = 0; j < publishCount; j++) {
                        String topic = "/topic" + (topicIndex.incrementAndGet() % topicCount);
                        client.publish(topic, MqttQoS.valueOf(qos), payload, false, false);
                    }
                    client.flush();
                } catch (Exception e) {
                    System.err.println("[bench-plugin] 发布异常: " + e.getMessage());
                }
            }, period, period, TimeUnit.MILLISECONDS);
        }

        // 定时打印TPS
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            System.out.println("[bench-plugin] 发布压测运行中...");
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 运行订阅压测
     */
    private void runSubscribeBenchmark(BrokerContext brokerContext, SubscribeConfig config) throws Exception {
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
            BrokerTopic topic = brokerContext.getOrCreateTopic(topicName);
        }
        System.out.println("[bench-plugin] 测试主题已创建");

        // 等待Broker启动
        Thread.sleep(1000);

        AsynchronousChannelGroup channelGroup = new EnhanceAsynchronousChannelProvider(
                Boolean.parseBoolean(System.getenv("BROKER_LOWMEMORY")))
                .openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(),
                        r -> new Thread(r, "bench-sub-pool"));

        CountDownLatch subscribeLatch = new CountDownLatch(connections * topicCount);
        MqttClient[] clients = new MqttClient[connections];
        CountDownLatch connectLatch = new CountDownLatch(connections);

        // 创建订阅连接
        for (int i = 0; i < connections; i++) {
            final int clientId = i;
            MqttClient client = new MqttClient(config.getHost(), config.getPort(),
                    opt -> opt.setGroup(channelGroup)
                            .setKeepAliveInterval(30)
                            .setAutomaticReconnect(true)
                            .setClientId("bench-sub-" + clientId));

            client.connect(mqttConnAckMessage -> {
                clients[clientId] = client;
                connectLatch.countDown();

                // 连接成功后订阅主题
                for (int j = 0; j < topicCount; j++) {
                    String topicName = "topic_" + random + "_" + j;
                    client.subscribe(topicName, MqttQoS.valueOf(qos),
                            (mqttClient, message) -> {
                                // 收到消息的回调
                            },
                            (mqttClient, mqttQoS) -> subscribeLatch.countDown()
                    );
                }
            });
        }

        connectLatch.await();
        System.out.println("[bench-plugin] 所有连接已建立");

        subscribeLatch.await();
        System.out.println("[bench-plugin] 所有订阅已完成");

        // 创建发布者来触发消息
        int publisherCount = config.getPublisherCount();
        int publishCount = config.getPublishCount();
        int publishPeriod = config.getPublishPeriod();

        if (publisherCount > 0) {
            System.out.println("[bench-plugin] 启动发布者数量: " + publisherCount);

            AsynchronousChannelGroup publisherGroup = new EnhanceAsynchronousChannelProvider(false)
                    .openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(),
                            r -> new Thread(r, "bench-publisher-pool"));

            AtomicInteger pubTopicIndex = new AtomicInteger();
            byte[] payload = new byte[config.getPayloadSize()];
            Arrays.fill(payload, (byte) 1);

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors());

            for (int i = 0; i < publisherCount; i++) {
                final int pubId = i;
                MqttClient publisher = new MqttClient(config.getHost(), config.getPort(),
                        opt -> opt.setGroup(publisherGroup)
                                .setKeepAliveInterval(30)
                                .setAutomaticReconnect(true)
                                .setClientId("bench-publisher-" + pubId));

                publisher.connect();

                executor.scheduleWithFixedDelay(() -> {
                    try {
                        for (int j = 0; j < publishCount; j++) {
                            String topic = "topic_" + random + "_" + (pubTopicIndex.incrementAndGet() % topicCount);
                            publisher.publish(topic, MqttQoS.AT_MOST_ONCE, payload, false, false);
                        }
                        publisher.flush();
                    } catch (Exception e) {
                        System.err.println("[bench-plugin] 发布者异常: " + e.getMessage());
                    }
                }, publishPeriod, publishPeriod, TimeUnit.MILLISECONDS);
            }
        }

        // 定时打印状态
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            System.out.println("[bench-plugin] 订阅压测运行中...");
        }, 10, 10, TimeUnit.SECONDS);
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
    public String pluginName() {
        return "bench-plugin";
    }
}