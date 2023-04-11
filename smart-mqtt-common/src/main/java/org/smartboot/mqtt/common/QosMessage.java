/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/3/27
 */
public class QosMessage {
    private final MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message;
    private final Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer;
    private boolean commit;

    public QosMessage(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        this.message = message;
        this.consumer = consumer;
    }

    public MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> getMessage() {
        return message;
    }

    public Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> getConsumer() {
        return consumer;
    }

    public boolean isCommit() {
        return commit;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }
}
