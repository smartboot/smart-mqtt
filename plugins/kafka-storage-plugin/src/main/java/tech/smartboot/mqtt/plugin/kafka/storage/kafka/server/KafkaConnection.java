package tech.smartboot.mqtt.plugin.kafka.storage.kafka.server;

class KafkaConnection {
    private long requestCount;

    long nextRequestCount() {
        return ++requestCount;
    }
}
