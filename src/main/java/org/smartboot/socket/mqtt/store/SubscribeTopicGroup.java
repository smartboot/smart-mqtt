package org.smartboot.socket.mqtt.store;

import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.common.Topic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class SubscribeTopicGroup {
    /**
     * 当前订阅的消费者
     */
    private final Map<MqttSession, SubscriberConsumeOffset> consumeOffsets = new ConcurrentHashMap<>();
    /**
     * 空闲状态的订阅者
     */
    private final ConcurrentLinkedQueue<SubscriberConsumeOffset> idleSubscribers = new ConcurrentLinkedQueue<>();
    private final Topic topic;

    public SubscribeTopicGroup(Topic topic) {
        this.topic = topic;
    }

    public Map<MqttSession, SubscriberConsumeOffset> getConsumeOffsets() {
        return consumeOffsets;
    }

    public ConcurrentLinkedQueue<SubscriberConsumeOffset> getIdleSubscribers() {
        return idleSubscribers;
    }

    public Topic getTopic() {
        return topic;
    }
}
