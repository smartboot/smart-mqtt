package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端订阅的Topic，可能是通配符订阅
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/13
 */
public class TopicFilterSubscriber {
    private final TopicToken topicFilterToken;

    private final MqttQoS mqttQoS;

    /**
     * 客户端订阅所匹配的Topic。通配符订阅时可能有多个
     */
    private final Map<String, TopicSubscriber> topicSubscribers;

    public TopicFilterSubscriber(TopicToken topicFilterToken, MqttQoS mqttQoS, TopicSubscriber topicSubscriber) {
        this.topicFilterToken = topicFilterToken;
        this.mqttQoS = mqttQoS;
        topicSubscribers = new HashMap<>();
        topicSubscribers.put(topicSubscriber.getTopic().getTopic(), topicSubscriber);
    }

    public TopicToken getTopicFilterToken() {
        return topicFilterToken;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public Map<String, TopicSubscriber> getTopicSubscribers() {
        return topicSubscribers;
    }
}
