package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.Topic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final AtomicInteger version = new AtomicInteger();
    /**
     * 当前Topic是否圈闭推送完成
     */
    private boolean pushing;

    public BrokerTopic(String topic) {
        super(topic);
    }

    public Map<MqttSession, TopicSubscriber> getConsumeOffsets() {
        return consumeOffsets;
    }

    public AtomicInteger getVersion() {
        return version;
    }

    public boolean isPushing() {
        return pushing;
    }

    public void setPushing(boolean pushing) {
        this.pushing = pushing;
    }
}
