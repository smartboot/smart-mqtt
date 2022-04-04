package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.Topic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broker端的Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class BrokerTopic extends Topic {
    /**
     * 当前订阅的消费者
     */
    private final Map<MqttSession, TopicSubscriber> consumeOffsets = new ConcurrentHashMap<>();

    public BrokerTopic(String topic) {
        super(topic);
    }

    public Map<MqttSession, TopicSubscriber> getConsumeOffsets() {
        return consumeOffsets;
    }

}
