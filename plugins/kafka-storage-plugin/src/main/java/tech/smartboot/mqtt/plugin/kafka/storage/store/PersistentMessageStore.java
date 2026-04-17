package tech.smartboot.mqtt.plugin.kafka.storage.store;

import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PersistentMessageStore {
    private final Path rootDir;
    private final Path topicsDir;
    private final PluginConfig.StorageConfig storageConfig;
    private final KafkaStorageMetrics metrics;
    private final Map<String, Integer> configuredTopicPartitions = new ConcurrentHashMap<>();
    private final Map<String, TopicLog> topics = new ConcurrentHashMap<>();

    private ConsumerOffsetStore offsetStore;

    public PersistentMessageStore(Path rootDir,
                                  PluginConfig.StorageConfig storageConfig,
                                  List<PluginConfig.TopicConfig> topicConfigs,
                                  KafkaStorageMetrics metrics) {
        this.rootDir = rootDir;
        this.topicsDir = rootDir.resolve("topics");
        this.storageConfig = storageConfig;
        this.metrics = metrics;
        if (topicConfigs != null) {
            for (PluginConfig.TopicConfig topicConfig : topicConfigs) {
                if (topicConfig.getName() != null && !topicConfig.getName().isEmpty()) {
                    configuredTopicPartitions.put(topicConfig.getName(), Math.max(1, topicConfig.getPartitions()));
                }
            }
        }
    }

    public synchronized void start() throws IOException {
        Files.createDirectories(rootDir);
        Files.createDirectories(topicsDir);
        offsetStore = new ConsumerOffsetStore(rootDir.resolve("consumer-offsets.properties"));
        loadExistingTopics();
        for (Map.Entry<String, Integer> entry : configuredTopicPartitions.entrySet()) {
            ensureTopic(entry.getKey());
        }
        metrics.setStoredBytes(totalStoredBytes());
    }

    public synchronized void close() throws IOException {
        for (TopicLog topicLog : topics.values()) {
            topicLog.close();
        }
    }

    public StoredMessage appendMqtt(String topic,
                                    byte[] value,
                                    short qos,
                                    boolean retained,
                                    String sourceClientId) throws IOException {
        TopicLog topicLog = ensureTopic(topic);
        PartitionLog.AppendResult result = topicLog.appendMqtt(value, qos, retained, sourceClientId);
        metrics.markMqttMessage(value == null ? 0 : value.length);
        metrics.addStoredBytes(result.getWrittenBytes());
        return result.getMessage();
    }

    public StoredMessage appendKafka(String topic,
                                     int partition,
                                     byte[] key,
                                     byte[] value,
                                     long timestamp,
                                     String sourceClientId) throws IOException {
        TopicLog topicLog = ensureTopic(topic);
        PartitionLog.AppendResult result = topicLog.appendKafka(partition, key, value, timestamp, sourceClientId);
        metrics.markKafkaProducedMessage(value == null ? 0 : value.length);
        metrics.addStoredBytes(result.getWrittenBytes());
        return result.getMessage();
    }

    public PartitionLog.FetchResult fetch(String topic, int partition, long offset, int maxBytes, int maxRecords) throws IOException {
        TopicLog topicLog = topics.get(topic);
        if (topicLog == null) {
            return new PartitionLog.FetchResult(Collections.<StoredMessage>emptyList(), 0, 0);
        }
        PartitionLog.FetchResult result = topicLog.fetch(partition, offset, maxBytes, maxRecords);
        int readBytes = 0;
        for (StoredMessage message : result.getMessages()) {
            readBytes += message.payloadBytes();
        }
        metrics.markFetch(result.getMessages().size(), readBytes);
        return result;
    }

    public PartitionLog.OffsetLookupResult lookupOffset(String topic, int partition, long timestamp) throws IOException {
        TopicLog topicLog = topics.get(topic);
        if (topicLog == null) {
            return new PartitionLog.OffsetLookupResult(0, -1);
        }
        return topicLog.lookupOffset(partition, timestamp);
    }

    public synchronized TopicLog ensureTopic(String topic) throws IOException {
        TopicLog existing = topics.get(topic);
        if (existing != null) {
            return existing;
        }
        int partitions = configuredTopicPartitions.containsKey(topic)
                ? configuredTopicPartitions.get(topic)
                : Math.max(1, storageConfig.getDefaultPartitionCount());
        Path topicDir = topicsDir.resolve(StorePaths.encodeTopic(topic));
        TopicLog created = TopicLog.create(topic, topicDir, storageConfig, metrics, partitions);
        topics.put(topic, created);
        return created;
    }

    public boolean containsTopic(String topic) {
        return topics.containsKey(topic);
    }

    public synchronized List<String> topicNames() {
        return new ArrayList<>(topics.keySet());
    }

    public synchronized int partitionCount(String topic) {
        TopicLog topicLog = topics.get(topic);
        return topicLog == null ? 0 : topicLog.getPartitionCount();
    }

    public synchronized long highWatermark(String topic, int partition) {
        TopicLog topicLog = topics.get(topic);
        return topicLog == null ? 0 : topicLog.highWatermark(partition);
    }

    public synchronized long logStartOffset(String topic, int partition) {
        TopicLog topicLog = topics.get(topic);
        return topicLog == null ? 0 : topicLog.logStartOffset(partition);
    }

    public void commitOffset(String groupId, String topic, int partition, long offset, String metadata) throws IOException {
        offsetStore.commit(groupId, topic, partition, offset, metadata);
        metrics.markOffsetCommit();
    }

    public ConsumerOffsetStore.OffsetInfo readCommittedOffset(String groupId, String topic, int partition) {
        return offsetStore.read(groupId, topic, partition);
    }

    public Map<ConsumerOffsetStore.TopicPartitionKey, ConsumerOffsetStore.OffsetInfo> listCommittedOffsets(String groupId) {
        return offsetStore.listGroupOffsets(groupId);
    }

    public synchronized void flushDue() throws IOException {
        long now = System.currentTimeMillis();
        for (TopicLog topicLog : topics.values()) {
            topicLog.flushIfRequired(now);
        }
    }

    public synchronized CleanupResult cleanupExpired() throws IOException {
        long retentionMillis = storageConfig.getRetentionHours() <= 0 ? -1 : storageConfig.getRetentionHours() * 60L * 60L * 1000L;
        int removedSegments = 0;
        for (TopicLog topicLog : topics.values()) {
            removedSegments += topicLog.cleanup(retentionMillis, storageConfig.getRetentionBytes());
        }
        if (removedSegments > 0) {
            metrics.markCleanup(removedSegments);
        }
        return new CleanupResult(removedSegments);
    }

    public int topicCount() {
        return topics.size();
    }

    public int consumerGroupCount() {
        return offsetStore.groupCount();
    }

    private void loadExistingTopics() throws IOException {
        try (Stream<Path> stream = Files.list(topicsDir)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                try {
                    TopicLog topicLog = TopicLog.load(path, storageConfig, metrics);
                    topics.put(topicLog.getTopic(), topicLog);
                } catch (IOException e) {
                    throw new IllegalStateException("load topic failed: " + path, e);
                }
            });
        }
    }

    private long totalStoredBytes() {
        long total = 0;
        for (TopicLog topicLog : topics.values()) {
            total += topicLog.sizeInBytes();
        }
        return total;
    }

    public static class CleanupResult {
        private final int removedSegments;

        CleanupResult(int removedSegments) {
            this.removedSegments = removedSegments;
        }

        public int getRemovedSegments() {
            return removedSegments;
        }
    }
}
