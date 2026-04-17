package tech.smartboot.mqtt.plugin.kafka.storage.store;

import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class PartitionLog {
    private final String topic;
    private final int partition;
    private final Path dir;
    private final PluginConfig.StorageConfig config;
    private final KafkaStorageMetrics metrics;
    private final TreeMap<Long, LogSegment> segments = new TreeMap<>();

    private long nextOffset;
    private long lastFlushTime = System.currentTimeMillis();

    public PartitionLog(String topic,
                        int partition,
                        Path dir,
                        PluginConfig.StorageConfig config,
                        KafkaStorageMetrics metrics) throws IOException {
        this.topic = topic;
        this.partition = partition;
        this.dir = dir;
        this.config = config;
        this.metrics = metrics;
        Files.createDirectories(dir);
        loadSegments();
    }

    private void loadSegments() throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            stream
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted()
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        long baseOffset = Long.parseLong(fileName.substring(0, fileName.length() - 4));
                        try {
                            segments.put(baseOffset, new LogSegment(path, baseOffset));
                        } catch (IOException e) {
                            throw new IllegalStateException("load segment failed: " + path, e);
                        }
                    });
        }
        if (!segments.isEmpty()) {
            LogSegment last = segments.lastEntry().getValue();
            nextOffset = last.lastOffset() + 1;
        }
    }

    public synchronized AppendResult append(byte[] key,
                                            byte[] value,
                                            long timestamp,
                                            short qos,
                                            boolean retained,
                                            String sourceClientId,
                                            byte sourceType) throws IOException {
        long offset = nextOffset;
        StoredMessage message = new StoredMessage(topic, partition, offset, timestamp, key, value, qos, retained, sourceClientId, sourceType);
        LogSegment active = activeSegment();
        if (active == null || shouldRoll(active)) {
            active = createSegment(offset);
        }
        LogSegment.AppendResult appendResult = active.append(message);
        nextOffset = offset + 1;
        if (config.isFlushOnEveryWrite()) {
            active.force();
            lastFlushTime = System.currentTimeMillis();
        }
        return new AppendResult(appendResult.getMessage(), appendResult.getWrittenBytes());
    }

    public synchronized FetchResult fetch(long offset, int maxBytes, int maxRecords) throws IOException {
        List<StoredMessage> messages = new ArrayList<>();
        int remainingBytes = maxBytes <= 0 ? Integer.MAX_VALUE : maxBytes;
        int remainingRecords = maxRecords <= 0 ? Integer.MAX_VALUE : maxRecords;
        Map.Entry<Long, LogSegment> start = segments.floorEntry(offset);
        if (start == null) {
            start = segments.ceilingEntry(offset);
        }
        if (start == null) {
            return new FetchResult(messages, nextOffset, logStartOffset());
        }
        for (LogSegment segment : segments.tailMap(start.getKey(), true).values()) {
            List<StoredMessage> part = segment.readFromOffset(topic, partition, offset, remainingBytes, remainingRecords);
            for (StoredMessage message : part) {
                messages.add(message);
                remainingBytes -= LogSegment.sizeOf(message);
                remainingRecords--;
            }
            if (remainingBytes <= 0 || remainingRecords <= 0) {
                break;
            }
            offset = segment.lastOffset() + 1;
        }
        return new FetchResult(messages, nextOffset, logStartOffset());
    }

    public synchronized OffsetLookupResult lookupOffset(long timestamp) throws IOException {
        if (segments.isEmpty()) {
            return new OffsetLookupResult(0, -1);
        }
        if (timestamp == org.apache.kafka.common.requests.ListOffsetsRequest.EARLIEST_TIMESTAMP) {
            return new OffsetLookupResult(logStartOffset(), -1);
        }
        if (timestamp == org.apache.kafka.common.requests.ListOffsetsRequest.LATEST_TIMESTAMP) {
            return new OffsetLookupResult(nextOffset, -1);
        }
        for (LogSegment segment : segments.values()) {
            LogSegment.OffsetLookupResult result = segment.findOffsetByTimestamp(topic, partition, timestamp);
            if (result.getTimestamp() >= 0) {
                return new OffsetLookupResult(result.getOffset(), result.getTimestamp());
            }
        }
        return new OffsetLookupResult(nextOffset, -1);
    }

    public synchronized int cleanup(long retentionMillis, long retentionBytes) throws IOException {
        int removed = 0;
        long totalBytes = sizeInBytes();
        long expireBefore = retentionMillis <= 0 ? Long.MIN_VALUE : System.currentTimeMillis() - retentionMillis;
        while (segments.size() > 1) {
            LogSegment oldest = segments.firstEntry().getValue();
            boolean expiredByTime = retentionMillis > 0 && oldest.lastAppendTime() < expireBefore;
            boolean expiredBySize = retentionBytes > 0 && totalBytes > retentionBytes;
            if (!expiredByTime && !expiredBySize) {
                break;
            }
            totalBytes -= oldest.getSizeInBytes();
            metrics.addStoredBytes(-oldest.getSizeInBytes());
            oldest.delete();
            segments.pollFirstEntry();
            removed++;
        }
        return removed;
    }

    public synchronized void flushIfRequired(long now) throws IOException {
        LogSegment active = activeSegment();
        if (active == null || !active.isDirty()) {
            return;
        }
        if (now - lastFlushTime >= config.getFlushIntervalMs()) {
            active.force();
            lastFlushTime = now;
        }
    }

    public synchronized long sizeInBytes() {
        long size = 0;
        for (LogSegment segment : segments.values()) {
            size += segment.getSizeInBytes();
        }
        return size;
    }

    public synchronized long nextOffset() {
        return nextOffset;
    }

    public synchronized long logStartOffset() {
        if (segments.isEmpty()) {
            return nextOffset;
        }
        return segments.firstEntry().getValue().firstOffset();
    }

    public synchronized void close() throws IOException {
        for (LogSegment segment : segments.values()) {
            segment.close();
        }
    }

    private boolean shouldRoll(LogSegment segment) {
        return segment.recordCount() > 0 && segment.getSizeInBytes() >= config.getSegmentBytes();
    }

    private LogSegment activeSegment() {
        return segments.isEmpty() ? null : segments.lastEntry().getValue();
    }

    private LogSegment createSegment(long baseOffset) throws IOException {
        Path segmentPath = dir.resolve(String.format("%020d.log", baseOffset));
        if (Files.exists(segmentPath)) {
            Path backup = dir.resolve(segmentPath.getFileName().toString() + ".bak");
            Files.move(segmentPath, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        LogSegment segment = new LogSegment(segmentPath, baseOffset);
        segments.put(baseOffset, segment);
        return segment;
    }

    public static class AppendResult {
        private final StoredMessage message;
        private final int writtenBytes;

        AppendResult(StoredMessage message, int writtenBytes) {
            this.message = message;
            this.writtenBytes = writtenBytes;
        }

        public StoredMessage getMessage() {
            return message;
        }

        public int getWrittenBytes() {
            return writtenBytes;
        }
    }

    public static class FetchResult {
        private final List<StoredMessage> messages;
        private final long highWatermark;
        private final long logStartOffset;

        FetchResult(List<StoredMessage> messages, long highWatermark, long logStartOffset) {
            this.messages = messages;
            this.highWatermark = highWatermark;
            this.logStartOffset = logStartOffset;
        }

        public List<StoredMessage> getMessages() {
            return messages;
        }

        public long getHighWatermark() {
            return highWatermark;
        }

        public long getLogStartOffset() {
            return logStartOffset;
        }
    }

    public static class OffsetLookupResult {
        private final long offset;
        private final long timestamp;

        OffsetLookupResult(long offset, long timestamp) {
            this.offset = offset;
            this.timestamp = timestamp;
        }

        public long getOffset() {
            return offset;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
