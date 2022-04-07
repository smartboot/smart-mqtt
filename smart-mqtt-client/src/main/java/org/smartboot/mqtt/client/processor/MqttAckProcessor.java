package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/7
 */
public class MqttAckProcessor<T extends MqttPacketIdentifierMessage> implements MqttProcessor<T> {
    @Override
    public void process(MqttClient mqttClient, T message) {
        mqttClient.notifyResponse(message);
    }
}
