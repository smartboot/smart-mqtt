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
