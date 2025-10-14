/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import tech.smartboot.mqtt.common.enums.MqttMessageType;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

import java.util.concurrent.CompletableFuture;

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

    private final CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = new CompletableFuture<>();
    /**
     * 飞行队列为其分配的packetId
     */
    private final int assignedPacketId;

    private MqttMessageType expectMessageType;

    private boolean commit;

    private int retryCount;

    private long latestTime;

    public InflightMessage(int packetId, MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> originalMessage) {
        this.assignedPacketId = packetId;
        this.originalMessage = originalMessage;
        if (originalMessage instanceof MqttPublishMessage) {
            if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.AT_LEAST_ONCE) {
                this.expectMessageType = MqttMessageType.PUBACK;
            } else if (originalMessage.getFixedHeader().getQosLevel() == MqttQoS.EXACTLY_ONCE) {
                this.expectMessageType = MqttMessageType.PUBREC;
            }
        } else if (originalMessage instanceof MqttSubscribeMessage) {
            this.expectMessageType = MqttMessageType.SUBACK;
        } else if (originalMessage instanceof MqttUnsubscribeMessage) {
            this.expectMessageType = MqttMessageType.UNSUBACK;
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

    public int getAssignedPacketId() {
        return assignedPacketId;
    }

    public MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> responseMessage) {
        this.responseMessage = responseMessage;
    }

    public CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> getFuture() {
        return future;
    }
}
