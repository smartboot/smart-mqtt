package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.MqttSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/28
 */
public interface SubscribeProvider {
    boolean subscribeTopic(String topicFilter, MqttSession session);
}
