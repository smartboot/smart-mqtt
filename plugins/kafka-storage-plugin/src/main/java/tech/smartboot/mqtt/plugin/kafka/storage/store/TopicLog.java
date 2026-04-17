package tech.smartboot.mqtt.plugin.kafka.storage.store;

import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class TopicLog {
    private static final String META_FILE = "topic.properties";

    private final String topic;
    private final Path dir;
    private final PartitionStrategy partitionStrategy;
    private final List<PartitionLog> partitions = new ArrayList<>();
    private final AtomicInteger rrCounter = new AtomicInteger();

    private TopicLog(String topic,
                     Path dir,
                     PluginConfig.StorageConfig config,
                     KafkaStorageMetrics metrics,
                     int partitionCount) throws IOException {
        this.topic = topic;
        this.dir = dir;
        this.partitionStrategy = PartitionStrategy.fromValue(config.getMqttPartitionStrategy());
        Files.createDirectories(dir);
        writeMeta(topic, partitionCount);
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(new PartitionLog(topic, i, dir.resolve("p-" + i), config, metrics));
        }
    }

    public static TopicLog create(String topic,
                                  Path dir,
                                  PluginConfig.StorageConfig config,
                                  KafkaStorageMetrics metrics,
                                  int partitionCount) throws IOException {
        return new TopicLog(topic, dir, config, metrics, partitionCount);
    }

    public static TopicLog load(Path dir,
                                PluginConfig.StorageConfig config,
                                KafkaStorageMetrics metrics) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(dir.resolve(META_FILE))) {
            properties.load(inputStream);
        }
        String topic = properties.getProperty("name");
        int partitionCount = Integer.parseInt(properties.getProperty("partitions"));
        return new TopicLog(topic, dir, config, metrics, partitionCount);
    }

    public String getTopic() {
        return topic;
    }

    public int getPartitionCount() {
        return partitions.size();
    }

    public synchronized PartitionLog.AppendResult appendMqtt(byte[] value,
                                                             short qos,
                                                             boolean retained,
                                                             String sourceClientId) throws IOException {
        return partition(nextPartition()).append(null, value, System.currentTimeMillis(), qos, retained, sourceClientId, StoredMessage.SOURCE_MQTT);
    }

    public synchronized PartitionLog.AppendResult appendKafka(int partition,
                                                              byte[] key,
                                                              byte[] value,
                                                              long timestamp,
                                                              String sourceClientId) throws IOException {
        return partition(partition).append(key, value, timestamp, (short) 1, false, sourceClientId, StoredMessage.SOURCE_KAFKA);
    }

    public synchronized PartitionLog.FetchResult fetch(int partition, long offset, int maxBytes, int maxRecords) throws IOException {
        return partition(partition).fetch(offset, maxBytes, maxRecords);
    }

    public synchronized PartitionLog.OffsetLookupResult lookupOffset(int partition, long timestamp) throws IOException {
        return partition(partition).lookupOffset(timestamp);
    }

    public synchronized int cleanup(long retentionMillis, long retentionBytes) throws IOException {
        int removed = 0;
        for (PartitionLog partitionLog : partitions) {
            removed += partitionLog.cleanup(retentionMillis, retentionBytes);
        }
        return removed;
    }

    public synchronized void flushIfRequired(long now) throws IOException {
        for (PartitionLog partitionLog : partitions) {
            partitionLog.flushIfRequired(now);
        }
    }

    public synchronized long sizeInBytes() {
        long size = 0;
        for (PartitionLog partitionLog : partitions) {
            size += partitionLog.sizeInBytes();
        }
        return size;
    }

    public synchronized void close() throws IOException {
        for (PartitionLog partitionLog : partitions) {
            partitionLog.close();
        }
    }

    public synchronized long highWatermark(int partition) {
        return partition(partition).nextOffset();
    }

    public synchronized long logStartOffset(int partition) {
        return partition(partition).logStartOffset();
    }

    private int nextPartition() {
        if (partitionStrategy == PartitionStrategy.TOPIC_HASH) {
            return Math.abs(topic.hashCode()) % partitions.size();
        }
        return Math.abs(rrCounter.getAndIncrement()) % partitions.size();
    }

    private PartitionLog partition(int partition) {
        if (partition < 0 || partition >= partitions.size()) {
            throw new IllegalArgumentException("invalid partition " + partition + " for topic " + topic);
        }
        return partitions.get(partition);
    }

    private void writeMeta(String topic, int partitionCount) throws IOException {
        Path file = dir.resolve(META_FILE);
        if (Files.exists(file)) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("name", topic);
        properties.setProperty("partitions", String.valueOf(partitionCount));
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            properties.store(outputStream, "kafka-storage-plugin");
        }
    }
}
