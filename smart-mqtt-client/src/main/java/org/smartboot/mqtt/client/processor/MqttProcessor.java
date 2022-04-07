package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public interface MqttProcessor<T extends MqttMessage> {

    /**
     * 处理Mqtt消息
     */
    void process(MqttClient mqttClient, T message);
}
