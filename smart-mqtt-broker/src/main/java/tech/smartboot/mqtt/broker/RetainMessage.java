package tech.smartboot.mqtt.broker;

/**
 * @author 三刀
 * @version v1.0 7/25/25
 */
final class RetainMessage {
    /**
     * 消息存储时间
     */
    private final long createTime = System.currentTimeMillis();
    /**
     * 负载数据
     */
    private final byte[] payload;

    /**
     * 主题
     */
    private final String topic;

    public RetainMessage(byte[] payload, String topic) {
        this.payload = payload;
        this.topic = topic;
    }

    public long getCreateTime() {
        return createTime;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }
}
