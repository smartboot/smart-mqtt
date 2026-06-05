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

    public RetainMessage(byte[] payload) {
        this.payload = payload;
    }

    public long getCreateTime() {
        return createTime;
    }

    public byte[] getPayload() {
        return payload;
    }

}
