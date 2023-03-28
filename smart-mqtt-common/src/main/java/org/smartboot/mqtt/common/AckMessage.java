package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttVariableMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/14
 */
public class AckMessage<T> {
    /**
     * 原始消息
     */
    private final MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> originalMessage;
    private MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> responseMessage;

    /**
     * 飞行队列为其分配的packetId
     */
    private final int assignedPacketId;

    private MqttMessageType expectMessageType;

    private boolean commit;

    private final InflightConsumer<T> consumer;

    private int retryCount;

    private long latestTime;
    private final T attach;

    public AckMessage(int packetId, MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> originalMessage, InflightConsumer<T> consumer, T attach) {
        this.assignedPacketId = packetId;
        this.originalMessage = originalMessage;
        this.consumer = consumer;
        this.attach = attach;
        if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
            this.expectMessageType = MqttMessageType.PUBACK;
        } else if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.EXACTLY_ONCE) {
            this.expectMessageType = MqttMessageType.PUBREC;
        }
        this.latestTime = System.currentTimeMillis();
    }

    public MqttVariableMessage<? extends MqttPacketIdVariableHeader> getOriginalMessage() {
        return originalMessage;
    }


    public MqttMessageType getExpectMessageType() {
        return expectMessageType;
    }

    public void setExpectMessageType(MqttMessageType expectMessageType) {
        this.expectMessageType = expectMessageType;
    }

    public boolean isCommit() {
        return commit;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
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

    public final InflightConsumer<T> getConsumer() {
        return consumer;
    }

    public int getAssignedPacketId() {
        return assignedPacketId;
    }

    public MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> responseMessage) {
        this.responseMessage = responseMessage;
    }

    public T getAttach() {
        return attach;
    }
}
