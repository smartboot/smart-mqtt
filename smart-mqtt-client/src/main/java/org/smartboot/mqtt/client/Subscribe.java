package org.smartboot.mqtt.client;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.BiConsumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/7
 */
public class Subscribe {
    private final String topicFilter;
    private final MqttQoS qoS;
    private final BiConsumer<MqttClient, MqttPublishMessage> consumer;

    public Subscribe(String topicFilter, MqttQoS qoS, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        this.topicFilter = topicFilter;
        this.qoS = qoS;
        this.consumer = consumer;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public MqttQoS getQoS() {
        return qoS;
    }

    public BiConsumer<MqttClient, MqttPublishMessage> getConsumer() {
        return consumer;
    }
}
