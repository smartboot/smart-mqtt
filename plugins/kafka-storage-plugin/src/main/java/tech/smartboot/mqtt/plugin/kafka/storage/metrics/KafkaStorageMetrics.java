package tech.smartboot.mqtt.plugin.kafka.storage.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 插件运行时指标。
 */
public class KafkaStorageMetrics {
    private final LongAdder mqttMessages = new LongAdder();
    private final LongAdder kafkaProducedMessages = new LongAdder();
    private final LongAdder fetchedMessages = new LongAdder();
    private final LongAdder committedOffsets = new LongAdder();
    private final LongAdder bytesWritten = new LongAdder();
    private final LongAdder bytesRead = new LongAdder();
    private final LongAdder cleanupRuns = new LongAdder();
    private final LongAdder deletedSegments = new LongAdder();
    private final LongAdder errors = new LongAdder();
    private final AtomicLong storedBytes = new AtomicLong();

    public void markMqttMessage(int bytes) {
        mqttMessages.increment();
        bytesWritten.add(bytes);
    }

    public void markKafkaProducedMessage(int bytes) {
        kafkaProducedMessages.increment();
        bytesWritten.add(bytes);
    }

    public void markFetch(int messageCount, int bytes) {
        fetchedMessages.add(messageCount);
        bytesRead.add(bytes);
    }

    public void markOffsetCommit() {
        committedOffsets.increment();
    }

    public void markCleanup(int removedSegments) {
        cleanupRuns.increment();
        deletedSegments.add(removedSegments);
    }

    public void markError() {
        errors.increment();
    }

    public void addStoredBytes(long delta) {
        storedBytes.addAndGet(delta);
    }

    public void setStoredBytes(long value) {
        storedBytes.set(value);
    }

    public Snapshot snapshot(int topicCount, int consumerGroupCount) {
        return new Snapshot(
                mqttMessages.sum(),
                kafkaProducedMessages.sum(),
                fetchedMessages.sum(),
                committedOffsets.sum(),
                bytesWritten.sum(),
                bytesRead.sum(),
                storedBytes.get(),
                cleanupRuns.sum(),
                deletedSegments.sum(),
                errors.sum(),
                topicCount,
                consumerGroupCount
        );
    }

    public static class Snapshot {
        private final long mqttMessages;
        private final long kafkaProducedMessages;
        private final long fetchedMessages;
        private final long committedOffsets;
        private final long bytesWritten;
        private final long bytesRead;
        private final long storedBytes;
        private final long cleanupRuns;
        private final long deletedSegments;
        private final long errors;
        private final int topicCount;
        private final int consumerGroupCount;

        public Snapshot(long mqttMessages,
                        long kafkaProducedMessages,
                        long fetchedMessages,
                        long committedOffsets,
                        long bytesWritten,
                        long bytesRead,
                        long storedBytes,
                        long cleanupRuns,
                        long deletedSegments,
                        long errors,
                        int topicCount,
                        int consumerGroupCount) {
            this.mqttMessages = mqttMessages;
            this.kafkaProducedMessages = kafkaProducedMessages;
            this.fetchedMessages = fetchedMessages;
            this.committedOffsets = committedOffsets;
            this.bytesWritten = bytesWritten;
            this.bytesRead = bytesRead;
            this.storedBytes = storedBytes;
            this.cleanupRuns = cleanupRuns;
            this.deletedSegments = deletedSegments;
            this.errors = errors;
            this.topicCount = topicCount;
            this.consumerGroupCount = consumerGroupCount;
        }

        @Override
        public String toString() {
            return "mqtt_messages=" + mqttMessages +
                    ", kafka_produced_messages=" + kafkaProducedMessages +
                    ", fetched_messages=" + fetchedMessages +
                    ", committed_offsets=" + committedOffsets +
                    ", bytes_written=" + bytesWritten +
                    ", bytes_read=" + bytesRead +
                    ", stored_bytes=" + storedBytes +
                    ", cleanup_runs=" + cleanupRuns +
                    ", deleted_segments=" + deletedSegments +
                    ", errors=" + errors +
                    ", active_topics=" + topicCount +
                    ", active_consumer_groups=" + consumerGroupCount;
        }
    }
}
