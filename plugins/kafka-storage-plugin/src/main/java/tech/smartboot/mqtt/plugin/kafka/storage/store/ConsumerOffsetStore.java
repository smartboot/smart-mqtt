package tech.smartboot.mqtt.plugin.kafka.storage.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConsumerOffsetStore {
    private final Path file;
    private final Map<String, OffsetInfo> offsets = new HashMap<>();

    public ConsumerOffsetStore(Path file) throws IOException {
        this.file = file;
        Files.createDirectories(file.getParent());
        load();
    }

    public synchronized void commit(String groupId, String topic, int partition, long offset, String metadata) throws IOException {
        OffsetInfo info = new OffsetInfo(groupId, topic, partition, offset, metadata == null ? "" : metadata, System.currentTimeMillis());
        offsets.put(key(groupId, topic, partition), info);
        persist();
    }

    public synchronized OffsetInfo read(String groupId, String topic, int partition) {
        return offsets.get(key(groupId, topic, partition));
    }

    public synchronized Map<TopicPartitionKey, OffsetInfo> listGroupOffsets(String groupId) {
        Map<TopicPartitionKey, OffsetInfo> result = new HashMap<>();
        for (OffsetInfo info : offsets.values()) {
            if (groupId.equals(info.getGroupId())) {
                result.put(new TopicPartitionKey(info.getTopic(), info.getPartition()), info);
            }
        }
        return result;
    }

    public synchronized int groupCount() {
        return (int) offsets.values().stream().map(OffsetInfo::getGroupId).distinct().count();
    }

    private void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        }
        for (String name : properties.stringPropertyNames()) {
            String[] value = properties.getProperty(name, "").split("\t", -1);
            if (value.length < 5) {
                continue;
            }
            String groupId = decode(value[0]);
            String topic = decode(value[1]);
            int partition = Integer.parseInt(value[2]);
            long offset = Long.parseLong(value[3]);
            String metadata = decode(value[4]);
            long commitTimestamp = value.length > 5 ? Long.parseLong(value[5]) : System.currentTimeMillis();
            offsets.put(name, new OffsetInfo(groupId, topic, partition, offset, metadata, commitTimestamp));
        }
    }

    private void persist() throws IOException {
        Properties properties = new Properties();
        for (Map.Entry<String, OffsetInfo> entry : offsets.entrySet()) {
            OffsetInfo value = entry.getValue();
            properties.setProperty(entry.getKey(),
                    encode(value.getGroupId()) + "\t" +
                            encode(value.getTopic()) + "\t" +
                            value.getPartition() + "\t" +
                            value.getOffset() + "\t" +
                            encode(value.getMetadata()) + "\t" +
                            value.getCommitTimestamp());
        }
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            properties.store(outputStream, "kafka-storage-plugin offsets");
        }
    }

    private static String key(String groupId, String topic, int partition) {
        return encode(groupId) + ":" + encode(topic) + ":" + partition;
    }

    private static String encode(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
    }

    public static class TopicPartitionKey {
        private final String topic;
        private final int partition;

        public TopicPartitionKey(String topic, int partition) {
            this.topic = topic;
            this.partition = partition;
        }

        public String getTopic() {
            return topic;
        }

        public int getPartition() {
            return partition;
        }
    }

    public static class OffsetInfo {
        private final String groupId;
        private final String topic;
        private final int partition;
        private final long offset;
        private final String metadata;
        private final long commitTimestamp;

        public OffsetInfo(String groupId, String topic, int partition, long offset, String metadata, long commitTimestamp) {
            this.groupId = groupId;
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.metadata = metadata;
            this.commitTimestamp = commitTimestamp;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getTopic() {
            return topic;
        }

        public int getPartition() {
            return partition;
        }

        public long getOffset() {
            return offset;
        }

        public String getMetadata() {
            return metadata;
        }

        public long getCommitTimestamp() {
            return commitTimestamp;
        }
    }
}
