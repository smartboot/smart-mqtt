package tech.smartboot.mqtt.plugin.kafka.storage;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttQoS;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaStoragePluginIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistMqttMessagesAndConsumeThemWithKafkaClient() throws Throwable {
        try (TestEnvironment environment = startEnvironment()) {
            CountDownLatch connectLatch = new CountDownLatch(1);
            MqttClient mqttClient = new MqttClient("127.0.0.1", environment.mqttPort);
            mqttClient.connect(connAck -> connectLatch.countDown());
            assertTrue(connectLatch.await(5, TimeUnit.SECONDS));

            CountDownLatch publishAck = new CountDownLatch(1);
            mqttClient.publish("telemetry/device", MqttQoS.AT_LEAST_ONCE, "hello-mqtt".getBytes(StandardCharsets.UTF_8), false, packetId -> publishAck.countDown(), true);
            assertTrue(publishAck.await(5, TimeUnit.SECONDS));

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProps(environment.kafkaPort, "mqtt-group"))) {
                TopicPartition tp = new TopicPartition("telemetry/device", 0);
                consumer.assign(Collections.singletonList(tp));
                consumer.seekToBeginning(Collections.singletonList(tp));
                ConsumerRecord<String, String> record = pollOne(consumer);
                assertNotNull(record);
                assertEquals("hello-mqtt", record.value());

                consumer.commitSync();
                assertTrue(consumer.committed(Collections.singleton(tp)).get(tp).offset() >= 1);
            }
            mqttClient.disconnect();
        }
    }

    @Test
    void shouldSupportKafkaProducerAndConsumerRoundTrip() throws Throwable {
        try (TestEnvironment environment = startEnvironment();
             KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerProps(environment.kafkaPort))) {

            producer.send(new ProducerRecord<String, String>("kafka-roundtrip", 0, "k1", "from-kafka")).get(5, TimeUnit.SECONDS);
            producer.flush();

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProps(environment.kafkaPort, "kafka-group"))) {
                TopicPartition tp = new TopicPartition("kafka-roundtrip", 0);
                consumer.assign(Collections.singletonList(tp));
                consumer.seekToBeginning(Collections.singletonList(tp));
                ConsumerRecord<String, String> record = pollOne(consumer);
                assertNotNull(record);
                assertEquals("k1", record.key());
                assertEquals("from-kafka", record.value());
            }
        }
    }

    private TestEnvironment startEnvironment() throws Throwable {
        int mqttPort = freePort();
        int kafkaPort = freePort();

        BrokerContextImpl brokerContext = new BrokerContextImpl();
        brokerContext.Options().setHost("127.0.0.1");
        brokerContext.Options().setPort(mqttPort);
        brokerContext.init();

        Path pluginStorage = tempDir.resolve("plugin-storage-" + kafkaPort);
        Files.createDirectories(pluginStorage);
        Files.write(pluginStorage.resolve("plugin.yaml"), (
                "kafka:\n" +
                        "  host: 127.0.0.1\n" +
                        "  port: " + kafkaPort + "\n" +
                        "  broker_id: 1\n" +
                        "  cluster_id: test-cluster\n" +
                        "  advertised_host: 127.0.0.1\n" +
                        "  advertised_port: " + kafkaPort + "\n" +
                        "storage:\n" +
                        "  data_path: data\n" +
                        "  auto_create_topics: true\n" +
                        "  default_partition_count: 1\n" +
                        "  flush_on_every_write: true\n" +
                        "  flush_interval_ms: 100\n" +
                        "  cleanup_interval_ms: 1000\n" +
                        "metrics:\n" +
                        "  log_interval_ms: 10000\n").getBytes(StandardCharsets.UTF_8));

        KafkaStoragePlugin plugin = new KafkaStoragePlugin();
        plugin.setStorage(pluginStorage.toFile());
        plugin.install(brokerContext);
        return new TestEnvironment(brokerContext, plugin, mqttPort, kafkaPort);
    }

    private static Properties producerProps(int kafkaPort) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "127.0.0.1:" + kafkaPort);
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());
        properties.put("acks", "1");
        properties.put("max.block.ms", "5000");
        return properties;
    }

    private static Properties consumerProps(int kafkaPort, String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:" + kafkaPort);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        return properties;
    }

    private static ConsumerRecord<String, String> pollOne(KafkaConsumer<String, String> consumer) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                return record;
            }
        }
        return null;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class TestEnvironment implements AutoCloseable {
        private final BrokerContextImpl brokerContext;
        private final KafkaStoragePlugin plugin;
        private final int mqttPort;
        private final int kafkaPort;

        private TestEnvironment(BrokerContextImpl brokerContext, KafkaStoragePlugin plugin, int mqttPort, int kafkaPort) {
            this.brokerContext = brokerContext;
            this.plugin = plugin;
            this.mqttPort = mqttPort;
            this.kafkaPort = kafkaPort;
        }

        @Override
        public void close() {
            plugin.uninstall();
            brokerContext.destroy();
        }
    }
}
