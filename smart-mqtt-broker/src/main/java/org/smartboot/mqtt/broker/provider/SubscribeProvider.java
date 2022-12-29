package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.MqttSession;

/**
 * Topic订阅
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/28
 */
public interface SubscribeProvider {
    boolean subscribeTopic(String topicFilter, MqttSession session);
}
