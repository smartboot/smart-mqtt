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
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.schema.Enum;
import tech.smartboot.mqtt.plugin.spec.schema.Item;
import tech.smartboot.mqtt.plugin.spec.schema.Schema;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * MQTT压测插件
 * 支持Publish和Subscribe两种压测场景
 *
 * @author 三刀
 * @version v1.5.1
 */
public class BenchPlugin extends Plugin {

    private final AtomicBoolean running = new AtomicBoolean(true);
    public final LongAdder subscribeCountAdder = new LongAdder();
    private final LongAdder publishCountAdder = new LongAdder();

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        running.set(true);
        PluginConfig config = loadPluginConfig(PluginConfig.class);
        ScenarioConfig scenarioConfig = config.getScenarios().stream().filter(scenario -> Objects.equals(scenario.getName(), config.getActive())).findFirst().orElse(null);
        if (scenarioConfig == null) {
            return;
        }
        new Thread(() -> {
            //延迟启动
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            AsynchronousChannelGroup group = null;
            try {
                group = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                    int i;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "bench-plugin-" + (++i));
                    }
                });
                runSubscribeBenchmark(brokerContext, config, scenarioConfig, group);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                log("压测结束");
                System.out.println("[bench-plugin] 压测结束");
                if (group != null) {
                    group.shutdown();
                }
            }
        }).start();
        new Thread(() -> {
            int expectPublishCount = scenarioConfig.getPublishers() * scenarioConfig.getRate() * 5;
            int expectSubscribeCount = expectPublishCount * scenarioConfig.getSubscribers() * 5;
            while (running.get()) {
                try {
                    Thread.sleep(5000);
                    int p = publishCountAdder.intValue();
                    publishCountAdder.add(-p);
                    int c = subscribeCountAdder.intValue();
                    subscribeCountAdder.add(-c);
                    String stats = String.format("消息数: %d, TPS: %d ,推送完成率: %f ,订阅触达率: %f", c, c / 5, p * 100.0 / expectPublishCount, expectSubscribeCount * 100.0 / expectPublishCount);
                    log(stats);
                    System.out.println("[bench-plugin] " + stats);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    protected void destroyPlugin() {
        log("开始优雅关闭...");
        System.out.println("[bench-plugin] 开始优雅关闭...");
        running.set(false);
    }


    /**
     * 运行订阅压测
     */
    private void runSubscribeBenchmark(BrokerContext brokerContext, PluginConfig config, ScenarioConfig scenario, AsynchronousChannelGroup group) throws Throwable {
        // 使用PluginConfig中的公共参数
        String host = config.getHost();
        int port = config.getPort();
        int topicCount = config.getTopicCount();
        int payloadSize = config.getPayloadSize();

        // 使用ScenarioConfig中的QoS参数
        int publishQos = scenario.getPublishQos();
        int subscribeQos = scenario.getSubscribeQos();

        // 输出压测配置信息
        String configInfo = String.format("压测配置 - 服务器: %s:%d, 主题数: %d, 负载大小: %d字节", host, port, topicCount, payloadSize);
        String scenarioInfo = String.format("场景配置 - 订阅者: %d, 发布者: %d, 每秒消息数/发布者: %d, 发布QoS: %d, 订阅QoS: %d", scenario.getSubscribers(), scenario.getPublishers(), scenario.getRate(), publishQos, subscribeQos);
        log(configInfo);
        log(scenarioInfo);
        System.out.println("[bench-plugin] " + configInfo);
        System.out.println("[bench-plugin] " + scenarioInfo);

        // 创建测试主题
        long random = System.currentTimeMillis() % 10000;
        for (int j = 0; j < topicCount; j++) {
            String topicName = "topic_" + random + "_" + j;
            brokerContext.getOrCreateTopic(topicName);
        }
        String topicInfo = "测试主题已创建完成，主题前缀: topic_" + random + "_*";
        log(topicInfo);
        System.out.println("[bench-plugin] " + topicInfo);

        List<MqttClient> clients = new ArrayList<>(scenario.getSubscribers());
        // 创建订阅连接
        for (int i = 0; i < scenario.getSubscribers(); i++) {
            final int clientId = i;
            MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(group).setKeepAliveInterval(30).setAutomaticReconnect(true).setClientId("bench-sub-" + clientId));
            clients.add(client);

            client.connect(mqttConnAckMessage -> {
                // 连接成功后订阅主题
                for (int j = 0; j < topicCount; j++) {
                    String topicName = "topic_" + random + "_" + j;
                    client.subscribe(topicName, MqttQoS.valueOf(subscribeQos), (mqttClient, message) -> {
                        // 收到消息的回调
                        subscribeCountAdder.increment();
                    });
                }
            });
        }
        String subInfo = "订阅者连接已建立: " + scenario.getSubscribers() + " 个";
        log(subInfo);
        System.out.println("[bench-plugin] " + subInfo);


        AtomicInteger pubTopicIndex = new AtomicInteger();
        byte[] payload = new byte[payloadSize];
        Arrays.fill(payload, (byte) 1);

        // 创建发布者线程
        List<MqttClient> publishers = new ArrayList<>(scenario.getPublishers());
        for (int i = 0; i < scenario.getPublishers(); i++) {
            final int pubId = i;
            MqttClient publisher = new MqttClient(host, port, opt -> opt.setGroup(group).setKeepAliveInterval(30).setAutomaticReconnect(true).setClientId("bench-publisher-" + pubId));
            publisher.connect();
            publishers.add(publisher);
        }
        String pubInfo = "发布者连接已建立: " + scenario.getPublishers() + " 个，压测开始...";
        log(pubInfo);
        System.out.println("[bench-plugin] " + pubInfo);

        long latestTime = 0;
        while (running.get()) {
            long wait = 1000 - System.currentTimeMillis() - latestTime;
            if (wait > 0) {
                Thread.sleep(wait);
            }
            latestTime = System.currentTimeMillis();
            for (MqttClient publisher : publishers) {
                try {
                    for (int j = 0; j < scenario.getRate(); j++) {
                        String topic = "topic_" + random + "_" + (pubTopicIndex.incrementAndGet() % topicCount);
                        publisher.publish(topic, MqttQoS.valueOf(publishQos), payload, false, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) {
                                publishCountAdder.increment();
                            }
                        }, false);
                    }
                    publisher.flush();
                } catch (Throwable e) {
                    String errMsg = "发布异常: " + e.getMessage();
                    log(errMsg);
                    System.out.println("[bench-plugin] " + errMsg);
                }

            }
        }
        publishers.forEach(MqttClient::disconnect);
        clients.forEach(MqttClient::disconnect);
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

        // 公共配置
        schema.addItem(Item.String("host", "MQTT服务器地址").tip("默认: 127.0.0.1").col(3));
        schema.addItem(Item.Int("port", "MQTT服务器端口").tip("默认: 1883").col(3));
        schema.addItem(Item.Int("payloadSize", "消息负载大小(字节)").tip("默认: 1024").col(3));
        schema.addItem(Item.Int("topicCount", "主题数量").tip("默认: 128").col(3));
        schema.addItem(Item.String("active", "激活的场景名称").col(6).tip("默认: default"));

        // 场景配置数组
        Item scenariosItem = Item.ItemArray("scenarios", "压测场景配置列表");
        scenariosItem.addItems(Item.String("name", "场景名称"), Item.Int("subscribers", "订阅者数量").tip("设置为0则不启动订阅者, 默认: 1000"), Item.Int("publishers", "发布者数量").tip("设置为0则不启动发布者, 默认: 1"), Item.Int("rate", "每秒推送消息数").tip("每个连接每秒推送的消息数, 默认: 1000"), Item.Int("publishQos", "发布QoS等级").tip("默认: 0").col(6).addEnums(Enum.of("0", "Qos0"), Enum.of("1", "Qos1"), Enum.of("2", "Qos2")), Item.Int("subscribeQos", "订阅QoS等级").tip("默认: 0").col(6).addEnums(Enum.of("0", "Qos0"), Enum.of("1", "Qos1"), Enum.of("2", "Qos2")));
        schema.addItem(scenariosItem);

        return schema;
    }

    @Override
    public String pluginName() {
        return "bench-plugin";
    }
}