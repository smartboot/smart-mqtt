package tech.smartboot.mqtt.plugin.kafka.storage;

import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.kafka.server.KafkaServer;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;
import tech.smartboot.mqtt.plugin.kafka.storage.store.PersistentMessageStore;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.schema.Enum;
import tech.smartboot.mqtt.plugin.spec.schema.Item;
import tech.smartboot.mqtt.plugin.spec.schema.Schema;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class KafkaStoragePlugin extends Plugin {
    private PluginConfig config;
    private KafkaStorageMetrics metrics;
    private PersistentMessageStore store;
    private KafkaServer kafkaServer;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        File pluginStorage;
        try {
            pluginStorage = storage();
        } catch (IllegalStateException e) {
            log("storage directory is not assigned, skip kafka-storage-plugin bootstrap for classpath-only loading");
            return;
        }
        config = loadPluginConfig(PluginConfig.class);
        if (config == null) {
            config = new PluginConfig();
        }
        metrics = new KafkaStorageMetrics();
        File dataDir = new File(pluginStorage, config.getStorage().getDataPath());
        Path root = dataDir.toPath();
        store = new PersistentMessageStore(root, config.getStorage(), config.getTopics(), metrics);
        store.start();

        kafkaServer = new KafkaServer(config.getKafka(), brokerContext, store, metrics);
        kafkaServer.start();
        addUsagePort(config.getKafka().getPort(), "kafka protocol port");

        consumer((session, message) -> {
            try {
                store.appendMqtt(
                        message.getTopic().getTopic(),
                        message.getPayload(),
                        (short) message.getQos().value(),
                        message.isRetained(),
                        session == null ? null : session.getClientId()
                );
            } catch (Exception e) {
                metrics.markError();
                e.printStackTrace();
            }
        });

        timer().scheduleWithFixedDelay(() -> {
            try {
                store.flushDue();
            } catch (Exception e) {
                metrics.markError();
                e.printStackTrace();
            }
        }, config.getStorage().getFlushIntervalMs(), TimeUnit.MILLISECONDS);

        timer().scheduleWithFixedDelay(() -> {
            try {
                store.cleanupExpired();
            } catch (Exception e) {
                metrics.markError();
                e.printStackTrace();
            }
        }, config.getStorage().getCleanupIntervalMs(), TimeUnit.MILLISECONDS);

        timer().scheduleWithFixedDelay(() -> {
            KafkaStorageMetrics.Snapshot snapshot = metrics.snapshot(store.topicCount(), store.consumerGroupCount());
            log("[kafka-storage-plugin] " + snapshot.toString());
        }, config.getMetrics().getLogIntervalMs(), TimeUnit.MILLISECONDS);

        log("kafka-storage-plugin started, kafka port: " + config.getKafka().getPort());
    }

    @Override
    protected void destroyPlugin() {
        try {
            if (kafkaServer != null) {
                kafkaServer.shutdown();
            }
            if (store != null) {
                store.flushDue();
                store.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    public String pluginName() {
        return "kafka-storage-plugin";
    }

    @Override
    public Schema schema() {
        Schema schema = new Schema();

        Item kafka = Item.Object("kafka", "Kafka 服务配置").col(12);
        kafka.addItems(
                Item.String("host", "监听地址").col(4),
                Item.Int("port", "监听端口").col(4),
                Item.Int("broker_id", "Broker ID").col(4),
                Item.String("cluster_id", "Cluster ID").col(4),
                Item.String("advertised_host", "对外地址").col(4),
                Item.Int("advertised_port", "对外端口").col(4),
                Item.Int("request_max_bytes", "请求最大字节数").col(4)
        );
        schema.addItem(kafka);

        Item storage = Item.Object("storage", "存储配置").col(12);
        storage.addItems(
                Item.String("data_path", "数据目录").col(4),
                Item.Switch("auto_create_topics", "自动创建 topic").col(4),
                Item.Int("default_partition_count", "默认分区数").col(4),
                Item.Int("segment_bytes", "分段大小").col(4),
                Item.Int("retention_bytes", "容量上限").col(4),
                Item.Int("retention_hours", "保留小时数").col(4),
                Item.Int("cleanup_interval_ms", "清理周期(ms)").col(4),
                Item.Int("flush_interval_ms", "刷盘周期(ms)").col(4),
                Item.Switch("flush_on_every_write", "每次写入立即刷盘").col(4),
                Item.String("mqtt_partition_strategy", "MQTT 分区策略")
                        .col(4)
                        .addEnums(Enum.of("round_robin", "round_robin"), Enum.of("topic_hash", "topic_hash"))
        );
        schema.addItem(storage);

        Item metricsItem = Item.Object("metrics", "指标配置").col(12);
        metricsItem.addItems(Item.Int("log_interval_ms", "日志输出周期(ms)").col(4));
        schema.addItem(metricsItem);

        Item topics = Item.ItemArray("topics", "预创建 topic").col(12);
        topics.addItems(
                Item.String("name", "topic 名称").col(8),
                Item.Int("partitions", "分区数").col(4)
        );
        schema.addItem(topics);
        return schema;
    }
}
