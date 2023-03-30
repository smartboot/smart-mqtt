package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/14
 */
public class InflightMessage {
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

    private final Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer;

    private int retryCount;

    private long latestTime;

    public InflightMessage(int packetId, MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> originalMessage, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        this.assignedPacketId = packetId;
        this.originalMessage = originalMessage;
        this.consumer = consumer;
        if (originalMessage instanceof MqttSubscribeMessage) {
            this.expectMessageType = MqttMessageType.SUBACK;
        } else if (originalMessage instanceof MqttUnsubscribeMessage) {
            this.expectMessageType = MqttMessageType.UNSUBACK;
        } else if (originalMessage instanceof MqttPublishMessage) {
            if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
                this.expectMessageType = MqttMessageType.PUBACK;
            } else if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.EXACTLY_ONCE) {
                this.expectMessageType = MqttMessageType.PUBREC;
            }
        } else {
            throw new UnsupportedOperationException();
        }

        this.latestTime = System.currentTimeMillis();
    }

    public MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> getOriginalMessage() {
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

    public final Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> getConsumer() {
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

}
