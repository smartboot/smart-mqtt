package org.smartboot.mqtt.broker.plugin.provider.impl.message;

import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public class PersistenceMessage extends ToString {
    /**
     * 负载数据
     */
    private final byte[] payload;
    /**
     * 主题
     */
    private final String topic;

    private final boolean retained;
    /**
     * 存储的消息偏移量
     */
    private final long offset;

    /**
     * 消息存储时间
     */
    private final long createTime = System.currentTimeMillis();

    public PersistenceMessage(MqttPublishMessage message, long offset) {
        this.payload = message.getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = message.getVariableHeader().getTopicName();
        this.offset = offset;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public boolean isRetained() {
        return retained;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getOffset() {
        return offset;
    }
}
