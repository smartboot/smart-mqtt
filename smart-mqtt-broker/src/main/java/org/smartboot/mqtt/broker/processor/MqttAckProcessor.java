package org.smartboot.mqtt.broker.processor;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.MqttVariableMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class MqttAckProcessor<T extends MqttVariableMessage<? extends MqttPacketIdVariableHeader>> extends AuthorizedMqttProcessor<T> {
    @Override
    public void process0(BrokerContext context, MqttSession session, T t) {
        session.notifyResponse(t);
    }
}
