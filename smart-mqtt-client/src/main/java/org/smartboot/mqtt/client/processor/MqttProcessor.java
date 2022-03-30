package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.broker.MqttContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public interface MqttProcessor<T extends MqttMessage> {

    /**
     * 处理Mqtt消息
     * @param session
     * @param t
     */
    void process(MqttClientSession session, T t);
}
