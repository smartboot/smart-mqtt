package tech.smartboot.mqtt.plugin.kafka.storage.store;

public enum PartitionStrategy {
    ROUND_ROBIN,
    TOPIC_HASH;

    public static PartitionStrategy fromValue(String value) {
        if (value == null) {
            return ROUND_ROBIN;
        }
        String normalized = value.trim().toUpperCase();
        if ("TOPIC_HASH".equals(normalized) || "HASH".equals(normalized)) {
            return TOPIC_HASH;
        }
        return ROUND_ROBIN;
    }
}
