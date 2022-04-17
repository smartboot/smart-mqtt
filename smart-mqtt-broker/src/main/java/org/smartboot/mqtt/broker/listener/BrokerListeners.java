package org.smartboot.mqtt.broker.listener;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.listener.MqttSessionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/5
 */
public class BrokerListeners {
    private final List<TopicEventListener> topicEventListeners = new ArrayList<>();
    private final List<BrokerLifecycleListener> brokerLifecycleListeners = new ArrayList<>();
    private final List<MqttSessionListener<MqttSession>> sessionListeners = new ArrayList<>();

    public List<TopicEventListener> getTopicEventListeners() {
        return topicEventListeners;
    }

    public List<BrokerLifecycleListener> getBrokerLifecycleListeners() {
        return brokerLifecycleListeners;
    }

    public List<MqttSessionListener<MqttSession>> getSessionListeners() {
        return sessionListeners;
    }
}
