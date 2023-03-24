package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/14
 */
public class AckMessage {
    /**
     * 原始消息
     */
    private final MqttPublishMessage originalMessage;

    private MqttMessageType expectMessageType;
    /**
     * 回调事件
     */
    private final Consumer<Long> consumer;

    private final long offset;

    private boolean commit;

    private final int packetId;

    private int retryCount;

    private long latestTime;

    public AckMessage(MqttPublishMessage originalMessage, int packetId, Consumer<Long> consumer, long offset) {
        this.originalMessage = originalMessage;
        this.consumer = consumer;
        this.offset = offset;
        this.packetId = packetId;
        if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
            this.expectMessageType = MqttMessageType.PUBACK;
        } else if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.EXACTLY_ONCE) {
            this.expectMessageType = MqttMessageType.PUBREC;
        }
        this.latestTime = System.currentTimeMillis();
    }

    public MqttPublishMessage getOriginalMessage() {
        return originalMessage;
    }


    public Consumer<Long> getConsumer() {
        return consumer;
    }

    public MqttMessageType getExpectMessageType() {
        return expectMessageType;
    }

    public void setExpectMessageType(MqttMessageType expectMessageType) {
        this.expectMessageType = expectMessageType;
    }

    public long getOffset() {
        return offset;
    }

    public boolean isCommit() {
        return commit;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public int getPacketId() {
        return packetId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(long latestTime) {
        this.latestTime = latestTime;
    }
}
