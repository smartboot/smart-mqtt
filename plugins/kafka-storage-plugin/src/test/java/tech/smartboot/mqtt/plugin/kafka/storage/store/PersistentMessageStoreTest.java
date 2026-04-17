package tech.smartboot.mqtt.plugin.kafka.storage.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PersistentMessageStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRecoverMessagesAndCommittedOffsetsAfterRestart() throws Exception {
        PluginConfig.StorageConfig config = new PluginConfig.StorageConfig();
        config.setDefaultPartitionCount(1);
        config.setFlushOnEveryWrite(true);

        PluginConfig.TopicConfig topicConfig = new PluginConfig.TopicConfig();
        topicConfig.setName("unit-topic");
        topicConfig.setPartitions(1);

        KafkaStorageMetrics metrics = new KafkaStorageMetrics();
        PersistentMessageStore store = new PersistentMessageStore(tempDir, config, Collections.singletonList(topicConfig), metrics);
        store.start();

        StoredMessage first = store.appendKafka("unit-topic", 0, "k1".getBytes(StandardCharsets.UTF_8), "v1".getBytes(StandardCharsets.UTF_8), 1L, "producer-a");
        store.commitOffset("group-a", "unit-topic", 0, first.getOffset() + 1, "done");
        store.close();

        KafkaStorageMetrics recoveredMetrics = new KafkaStorageMetrics();
        PersistentMessageStore recovered = new PersistentMessageStore(tempDir, config, Collections.singletonList(topicConfig), recoveredMetrics);
        recovered.start();

        PartitionLog.FetchResult fetchResult = recovered.fetch("unit-topic", 0, 0, 1024, 10);
        assertEquals(1, fetchResult.getMessages().size());
        assertEquals("v1", new String(fetchResult.getMessages().get(0).getValue(), StandardCharsets.UTF_8));
        assertEquals(first.getOffset(), fetchResult.getMessages().get(0).getOffset());

        ConsumerOffsetStore.OffsetInfo offsetInfo = recovered.readCommittedOffset("group-a", "unit-topic", 0);
        assertNotNull(offsetInfo);
        assertEquals(first.getOffset() + 1, offsetInfo.getOffset());
        recovered.close();
    }
}
