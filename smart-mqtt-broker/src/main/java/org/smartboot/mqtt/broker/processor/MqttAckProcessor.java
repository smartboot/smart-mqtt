package org.smartboot.mqtt.broker.processor;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class MqttAckProcessor<T extends MqttPacketIdentifierMessage> extends AuthorizedMqttProcessor<T> {
    @Override
    public void process0(BrokerContext context, MqttSession session, T t) {
        session.notifyResponse(t);
    }
}
