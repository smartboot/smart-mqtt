package tech.smartboot.mqtt.plugin.kafka.storage.store;

/**
 * 插件内部统一的持久化消息模型。
 */
public class StoredMessage {
    public static final byte SOURCE_MQTT = 0;
    public static final byte SOURCE_KAFKA = 1;

    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final byte[] key;
    private final byte[] value;
    private final short qos;
    private final boolean retained;
    private final String sourceClientId;
    private final byte sourceType;

    public StoredMessage(String topic,
                         int partition,
                         long offset,
                         long timestamp,
                         byte[] key,
                         byte[] value,
                         short qos,
                         boolean retained,
                         String sourceClientId,
                         byte sourceType) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
        this.qos = qos;
        this.retained = retained;
        this.sourceClientId = sourceClientId;
        this.sourceType = sourceType;
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

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public short getQos() {
        return qos;
    }

    public boolean isRetained() {
        return retained;
    }

    public String getSourceClientId() {
        return sourceClientId;
    }

    public byte getSourceType() {
        return sourceType;
    }

    public int payloadBytes() {
        return value == null ? 0 : value.length;
    }
}
