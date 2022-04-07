package org.smartboot.mqtt.client;

import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/7
 */
public interface TopicListener {
    void subscribe(String topicFilter, MqttQoS mqttQoS);

    void unsubscribe(String topicFilter);
}
