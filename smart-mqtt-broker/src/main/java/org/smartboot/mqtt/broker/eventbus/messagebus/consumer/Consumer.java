package org.smartboot.mqtt.broker.eventbus.messagebus.consumer;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public interface Consumer {
    void consume(BrokerContext brokerContext, MqttPublishMessage publishMessage);
}
