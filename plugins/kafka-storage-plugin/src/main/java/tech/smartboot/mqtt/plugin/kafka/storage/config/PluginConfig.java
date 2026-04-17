package tech.smartboot.mqtt.plugin.kafka.storage.config;

import java.util.ArrayList;
import java.util.List;

/**
 * kafka-storage-plugin 配置模型
 */
public class PluginConfig {
    private KafkaConfig kafka = new KafkaConfig();
    private StorageConfig storage = new StorageConfig();
    private MetricsConfig metrics = new MetricsConfig();
    private List<TopicConfig> topics = new ArrayList<>();

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public List<TopicConfig> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicConfig> topics) {
        this.topics = topics;
    }

    public static class KafkaConfig {
        private String host = "0.0.0.0";
        private int port = 9092;
        private int brokerId = 1;
        private String clusterId = "smart-mqtt-kafka";
        private String advertisedHost = "127.0.0.1";
        private int advertisedPort = 9092;
        private int requestMaxBytes = 10 * 1024 * 1024;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getBrokerId() {
            return brokerId;
        }

        public void setBrokerId(int brokerId) {
            this.brokerId = brokerId;
        }

        public String getClusterId() {
            return clusterId;
        }

        public void setClusterId(String clusterId) {
            this.clusterId = clusterId;
        }

        public String getAdvertisedHost() {
            return advertisedHost;
        }

        public void setAdvertisedHost(String advertisedHost) {
            this.advertisedHost = advertisedHost;
        }

        public int getAdvertisedPort() {
            return advertisedPort;
        }

        public void setAdvertisedPort(int advertisedPort) {
            this.advertisedPort = advertisedPort;
        }

        public int getRequestMaxBytes() {
            return requestMaxBytes;
        }

        public void setRequestMaxBytes(int requestMaxBytes) {
            this.requestMaxBytes = requestMaxBytes;
        }
    }

    public static class StorageConfig {
        private String dataPath = "data";
        private boolean autoCreateTopics = true;
        private int defaultPartitionCount = 3;
        private long segmentBytes = 64L * 1024 * 1024;
        private long retentionBytes = 1024L * 1024 * 1024;
        private long retentionHours = 168;
        private long cleanupIntervalMs = 60_000;
        private long flushIntervalMs = 1_000;
        private boolean flushOnEveryWrite = false;
        private String mqttPartitionStrategy = "round_robin";

        public String getDataPath() {
            return dataPath;
        }

        public void setDataPath(String dataPath) {
            this.dataPath = dataPath;
        }

        public boolean isAutoCreateTopics() {
            return autoCreateTopics;
        }

        public void setAutoCreateTopics(boolean autoCreateTopics) {
            this.autoCreateTopics = autoCreateTopics;
        }

        public int getDefaultPartitionCount() {
            return defaultPartitionCount;
        }

        public void setDefaultPartitionCount(int defaultPartitionCount) {
            this.defaultPartitionCount = defaultPartitionCount;
        }

        public long getSegmentBytes() {
            return segmentBytes;
        }

        public void setSegmentBytes(long segmentBytes) {
            this.segmentBytes = segmentBytes;
        }

        public long getRetentionBytes() {
            return retentionBytes;
        }

        public void setRetentionBytes(long retentionBytes) {
            this.retentionBytes = retentionBytes;
        }

        public long getRetentionHours() {
            return retentionHours;
        }

        public void setRetentionHours(long retentionHours) {
            this.retentionHours = retentionHours;
        }

        public long getCleanupIntervalMs() {
            return cleanupIntervalMs;
        }

        public void setCleanupIntervalMs(long cleanupIntervalMs) {
            this.cleanupIntervalMs = cleanupIntervalMs;
        }

        public long getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }

        public boolean isFlushOnEveryWrite() {
            return flushOnEveryWrite;
        }

        public void setFlushOnEveryWrite(boolean flushOnEveryWrite) {
            this.flushOnEveryWrite = flushOnEveryWrite;
        }

        public String getMqttPartitionStrategy() {
            return mqttPartitionStrategy;
        }

        public void setMqttPartitionStrategy(String mqttPartitionStrategy) {
            this.mqttPartitionStrategy = mqttPartitionStrategy;
        }
    }

    public static class MetricsConfig {
        private long logIntervalMs = 30_000;

        public long getLogIntervalMs() {
            return logIntervalMs;
        }

        public void setLogIntervalMs(long logIntervalMs) {
            this.logIntervalMs = logIntervalMs;
        }
    }

    public static class TopicConfig {
        private String name;
        private int partitions = 1;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }
    }
}
