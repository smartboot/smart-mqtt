package org.smartboot.mqtt.common.inflight;

import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/3/28
 */
public interface InflightConsumer<T> {
    void accept(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message, T attach);
}
